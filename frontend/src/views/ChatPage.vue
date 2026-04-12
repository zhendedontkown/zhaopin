<script setup lang="ts">
import { Client } from '@stomp/stompjs'
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import client from '../api/client'
import { useAuthStore } from '../stores/auth'
import type { ChatMessageRecord, ConversationRecord } from '../types'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const copy = {
  sidebarTitle: '\u804a\u5929\u5bf9\u8c61',
  sidebarSubtitle: '\u9009\u62e9\u4e00\u4e2a\u5bf9\u8bdd\u5bf9\u8c61\u540e\u67e5\u770b\u5b8c\u6574\u8bb0\u5f55',
  sidebarEmpty:
    '\u6682\u65f6\u8fd8\u6ca1\u6709\u53ef\u8fdb\u5165\u7684\u4f1a\u8bdd\uff0c\u6c42\u804c\u8005\u53ef\u4ee5\u4ece\u5c97\u4f4d\u8be6\u60c5\u76f4\u63a5\u53d1\u8d77\u5728\u7ebf\u6c9f\u901a\u3002',
  noSelected: '\u8bf7\u5148\u4ece\u5de6\u4fa7\u9009\u62e9\u804a\u5929\u5bf9\u8c61',
  noMessages: '\u8fd8\u6ca1\u6709\u804a\u5929\u8bb0\u5f55\uff0c\u5148\u53d1\u9001\u7b2c\u4e00\u6761\u6d88\u606f\u5427',
  emptyConversationLabel: '\u672a\u9009\u62e9\u4f1a\u8bdd',
  fallbackPreview: '\u70b9\u51fb\u8fdb\u5165\u4f1a\u8bdd',
  noMessageTime: '\u6682\u65e0\u65f6\u95f4',
  windowTitle: '\u804a\u5929\u5185\u5bb9',
  windowHint:
    '\u53f3\u4fa7\u6d88\u606f\u7a97\u53e3\u4f1a\u4fdd\u6301\u72ec\u7acb\u6eda\u52a8\uff0c\u53ef\u4ee5\u4e0a\u4e0b\u67e5\u770b\u5b8c\u6574\u804a\u5929\u8bb0\u5f55\u3002',
  placeholder: '\u8f93\u5165\u6d88\u606f\u5185\u5bb9\uff0c\u70b9\u51fb\u53f3\u4e0b\u89d2\u6309\u94ae\u53d1\u9001\u3002',
  send: '\u53d1\u9001\u6d88\u606f',
  sendError: '\u6d88\u606f\u53d1\u9001\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002',
  timeFormat: '\u65f6\u95f4\u683c\u5f0f\uff1a\u5e74-\u6708-\u65e5 \u65f6:\u5206',
  sideHint:
    '\u672c\u7aef\u53d1\u9001\u7684\u6d88\u606f\u663e\u793a\u5728\u53f3\u4fa7\uff0c\u5bf9\u65b9\u53d1\u9001\u7684\u6d88\u606f\u663e\u793a\u5728\u5de6\u4fa7\u3002',
} as const

const conversations = ref<ConversationRecord[]>([])
const messages = ref<ChatMessageRecord[]>([])
const selectedConversationId = ref<number | null>(null)
const draft = ref('')
const sending = ref(false)
const messageListRef = ref<HTMLElement | null>(null)
let stompClient: Client | null = null

const selectedConversation = computed(
  () => conversations.value.find((item) => item.id === selectedConversationId.value) ?? null,
)
const currentUserId = computed(() =>
  Number(authStore.profile?.userId ?? authStore.session?.userId ?? 0),
)
const currentDisplayName = computed(() =>
  authStore.profile?.jobseekerProfile?.fullName
  || authStore.profile?.companyProfile?.companyName
  || authStore.profile?.displayName
  || authStore.session?.displayName
  || '',
)
const selectedConversationLabel = computed(
  () => getConversationLabel(selectedConversation.value) || copy.emptyConversationLabel,
)

function isSelfMessage(message: ChatMessageRecord) {
  return String(message.senderUserId) === String(currentUserId.value)
}

function formatDateTime(value?: string) {
  if (!value) return ''
  const normalized = value.replace('T', ' ').replace(/\.\d+$/, '')
  const match = normalized.match(/^(\d{4}-\d{2}-\d{2}) (\d{2}):(\d{2})/)
  if (match) {
    return `${match[1]} ${match[2]}:${match[3]}`
  }
  return normalized.slice(0, 16)
}

function buildPreview(message?: string) {
  if (!message) return copy.fallbackPreview
  return message.length > 26 ? `${message.slice(0, 26)}...` : message
}

function getConversationLabel(conversation?: ConversationRecord | null) {
  if (!conversation) return ''

  const peerIsCompany = conversation.peerUserId === conversation.companyUserId
  const peerIsJobseeker = conversation.peerUserId === conversation.jobseekerUserId

  if (peerIsCompany && conversation.companyName) {
    return conversation.companyName
  }

  if (peerIsJobseeker && conversation.jobseekerName) {
    return conversation.jobseekerName
  }

  if (authStore.role === 'JOBSEEKER' && conversation.companyName) {
    return conversation.companyName
  }

  if (authStore.role === 'COMPANY' && conversation.jobseekerName) {
    return conversation.jobseekerName
  }

  if (conversation.peerName && conversation.peerName !== currentDisplayName.value) {
    return conversation.peerName
  }

  return conversation.companyName || conversation.jobseekerName || conversation.peerName || ''
}

async function fetchConversations() {
  try {
    const response = await client.get('/messages/conversations')
    conversations.value = response.data as ConversationRecord[]

    const peerUserId = Number(route.query.peerUserId || 0)
    if (peerUserId) {
      await ensureConversation(peerUserId)
      return
    }

    if (
      selectedConversationId.value &&
      !conversations.value.some((item) => item.id === selectedConversationId.value)
    ) {
      selectedConversationId.value = null
    }

    if (!selectedConversationId.value && conversations.value.length) {
      selectedConversationId.value = conversations.value[0].id
    }
  } catch {
    conversations.value = []
  }
}

async function ensureConversation(peerUserId: number) {
  try {
    const response = await client.post(`/messages/conversations/${peerUserId}`)
    const conversation = response.data as ConversationRecord
    const exists = conversations.value.find((item) => item.id === conversation.id)

    if (!exists) {
      conversations.value.unshift(conversation)
    } else {
      Object.assign(exists, conversation)
    }

    selectedConversationId.value = conversation.id

    if (route.query.peerUserId) {
      await router.replace({ path: route.path, query: {} })
    }
  } catch {
    // Shared axios interceptor already provides feedback.
  }
}

async function fetchMessages() {
  if (!selectedConversationId.value) {
    messages.value = []
    return
  }

  try {
    const response = await client.get(`/messages/conversations/${selectedConversationId.value}`)
    messages.value = response.data as ChatMessageRecord[]
    await scrollToBottom()
  } catch {
    messages.value = []
  }
}

function upsertIncomingMessage(message: ChatMessageRecord) {
  const exists = messages.value.some((item) => item.id === message.id)
  if (!exists && message.conversationId === selectedConversationId.value) {
    messages.value.push(message)
    void scrollToBottom()
  }
}

async function sendMessage() {
  if (!selectedConversation.value || !draft.value.trim()) return

  sending.value = true
  try {
    const response = await client.post(
      `/messages/conversations/${selectedConversation.value.id}/messages`,
      {
        receiverId: selectedConversation.value.peerUserId,
        content: draft.value.trim(),
      },
    )
    const savedMessage = response.data as ChatMessageRecord
    upsertIncomingMessage(savedMessage)
    draft.value = ''
    await fetchConversations()
  } catch {
    ElMessage.error(copy.sendError)
  } finally {
    sending.value = false
  }
}

async function scrollToBottom() {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

function connectSocket() {
  if (!authStore.session?.token || stompClient?.active) return

  stompClient = new Client({
    brokerURL: `${window.location.origin.replace('http', 'ws')}/ws/chat`,
    connectHeaders: {
      Authorization: `Bearer ${authStore.session.token}`,
    },
    reconnectDelay: 3000,
  })

  stompClient.onConnect = () => {
    stompClient?.subscribe('/user/queue/chat', async (frame) => {
      const message = JSON.parse(frame.body) as ChatMessageRecord
      upsertIncomingMessage(message)
      await fetchConversations()
    })
  }

  stompClient.activate()
}

watch(selectedConversationId, fetchMessages)

watch(
  () => route.query.peerUserId,
  async (value, oldValue) => {
    if (value && value !== oldValue) {
      await fetchConversations()
    }
  },
)

onMounted(async () => {
  await fetchConversations()
  connectSocket()
})

onUnmounted(() => {
  stompClient?.deactivate()
  stompClient = null
})
</script>

<template>
  <section class="chat-frame surface-card">
    <aside class="chat-sidebar">
      <div class="chat-sidebar__header">
        <span class="eyebrow">会话列表</span>
        <h2>{{ copy.sidebarTitle }}</h2>
      </div>

      <div class="chat-sidebar__list">
        <button
          v-for="item in conversations"
          :key="item.id"
          :class="['chat-contact', selectedConversationId === item.id ? 'is-active' : '']"
          @click="selectedConversationId = item.id"
        >
          <div class="chat-contact__head">
            <strong>{{ getConversationLabel(item) }}</strong>
            <small>{{ formatDateTime(item.lastMessageAt) || copy.noMessageTime }}</small>
          </div>
          <p>{{ buildPreview(item.lastMessage) }}</p>
        </button>

        <el-empty v-if="!conversations.length" :description="copy.sidebarEmpty" />
      </div>
    </aside>

    <section class="chat-window">
      <header class="chat-window__header">
        <div>
          <span class="eyebrow">聊天窗口</span>
          <h2>{{ selectedConversationLabel }}</h2>
        </div>
      </header>

      <div ref="messageListRef" class="chat-window__feed">
        <el-empty v-if="!selectedConversationId" :description="copy.noSelected" />
        <el-empty v-else-if="!messages.length" :description="copy.noMessages" />

        <div
          v-for="item in messages"
          :key="item.id"
          :class="['chat-row', isSelfMessage(item) ? 'self' : 'peer']"
        >
          <article class="chat-bubble">
            <p>{{ item.content }}</p>
            <span>{{ formatDateTime(item.createdAt) }}</span>
          </article>
        </div>
      </div>

      <footer class="chat-window__composer">
        <el-input
          v-model="draft"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          :placeholder="copy.placeholder"
        />

        <div class="chat-window__actions">
          <span>{{ copy.windowTitle }}: {{ selectedConversationLabel }}</span>
          <el-button
            type="primary"
            :loading="sending"
            :disabled="!selectedConversationId"
            @click="sendMessage"
          >
            {{ copy.send }}
          </el-button>
        </div>
      </footer>
    </section>
  </section>
</template>

<style scoped>
.chat-frame {
  height: calc(100vh - 150px);
  min-height: 700px;
  padding: 0;
  display: flex;
  overflow: hidden;
}

.chat-sidebar {
  width: 320px;
  min-width: 320px;
  border-right: 1px solid rgba(148, 163, 184, 0.14);
  background: rgba(248, 251, 255, 0.78);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-sidebar__header {
  padding: 22px 20px 18px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.14);
  display: grid;
  gap: 8px;
}

.chat-sidebar__header h2,
.chat-window__header h2 {
  margin: 0;
  font-size: 28px;
  line-height: 1.1;
  color: var(--text-strong);
}

.chat-sidebar__header p,
.chat-window__header p,
.chat-contact p,
.chat-contact small,
.chat-window__meta,
.chat-window__actions span,
.chat-bubble span {
  color: var(--text-muted);
}

.chat-sidebar__header p,
.chat-window__header p {
  margin: 0;
  line-height: 1.6;
}

.chat-sidebar__list {
  flex: 1;
  min-height: 0;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow-y: auto;
}

.chat-contact {
  width: 100%;
  padding: 14px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.9);
  text-align: left;
  display: grid;
  gap: 8px;
  cursor: pointer;
  transition:
    transform 180ms ease,
    border-color 180ms ease,
    box-shadow 180ms ease,
    background 180ms ease;
}

.chat-contact:hover {
  transform: translateY(-1px);
  border-color: rgba(3, 105, 161, 0.22);
  box-shadow: 0 12px 24px rgba(3, 105, 161, 0.08);
}

.chat-contact.is-active {
  background: rgba(226, 242, 255, 0.96);
  border-color: rgba(3, 105, 161, 0.26);
  box-shadow: 0 16px 30px rgba(3, 105, 161, 0.08);
}

.chat-contact__head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.chat-contact__head strong {
  font-size: 15px;
  color: var(--text-strong);
}

.chat-contact p {
  margin: 0;
  line-height: 1.5;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.chat-window {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.64);
}

.chat-window__header {
  padding: 22px 26px 18px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.14);
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.chat-window__meta {
  display: grid;
  gap: 8px;
  justify-items: end;
  max-width: 320px;
  font-size: 12px;
  text-align: right;
}

.chat-window__feed {
  flex: 1;
  min-height: 0;
  padding: 24px 26px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow-y: auto;
  scroll-behavior: smooth;
}

.chat-row {
  display: flex;
}

.chat-row.self {
  justify-content: flex-end;
}

.chat-bubble {
  max-width: min(70%, 620px);
  padding: 14px 16px;
  border-radius: 22px;
  background: #eef4f7;
  border: 1px solid rgba(148, 163, 184, 0.12);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.05);
}

.chat-row.self .chat-bubble {
  background: linear-gradient(135deg, #0369a1, #0284c7);
  border-color: transparent;
  color: white;
  box-shadow: 0 16px 32px rgba(2, 132, 199, 0.2);
}

.chat-bubble p {
  margin: 0;
  line-height: 1.72;
  word-break: break-word;
}

.chat-bubble span {
  display: block;
  margin-top: 8px;
  font-size: 12px;
  opacity: 0.82;
}

.chat-row.self .chat-bubble span {
  color: rgba(255, 255, 255, 0.82);
}

.chat-window__composer {
  padding: 18px 26px 22px;
  border-top: 1px solid rgba(148, 163, 184, 0.14);
  display: grid;
  gap: 12px;
  background: rgba(255, 255, 255, 0.78);
}

.chat-window__actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.chat-sidebar__list::-webkit-scrollbar,
.chat-window__feed::-webkit-scrollbar {
  width: 8px;
}

.chat-sidebar__list::-webkit-scrollbar-thumb,
.chat-window__feed::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.38);
}

@media (max-width: 1100px) {
  .chat-frame {
    height: auto;
    min-height: auto;
    flex-direction: column;
  }

  .chat-sidebar {
    width: 100%;
    min-width: 0;
    border-right: none;
    border-bottom: 1px solid rgba(148, 163, 184, 0.14);
  }

  .chat-sidebar__list {
    max-height: 280px;
  }

  .chat-window__feed {
    min-height: 420px;
  }
}

@media (max-width: 760px) {
  .chat-window__header,
  .chat-window__actions,
  .chat-contact__head {
    flex-direction: column;
    align-items: flex-start;
  }

  .chat-window__meta {
    justify-items: start;
    text-align: left;
    max-width: none;
  }

  .chat-bubble {
    max-width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .chat-contact,
  .chat-window__feed {
    transition: none;
    scroll-behavior: auto;
  }
}
</style>
