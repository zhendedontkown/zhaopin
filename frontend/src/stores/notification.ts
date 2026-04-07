import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { Client } from '@stomp/stompjs'
import client from '../api/client'
import type { NotificationRecord } from '../types'

let stompClient: Client | null = null

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref<NotificationRecord[]>([])

  const unreadCount = computed(() => notifications.value.filter((item) => item.readFlag === 0).length)

  async function fetchNotifications() {
    const response = await client.get('/notifications')
    notifications.value = response.data as NotificationRecord[]
  }

  async function markRead(id: number) {
    await client.patch(`/notifications/${id}/read`)
    const target = notifications.value.find((item) => item.id === id)
    if (target) target.readFlag = 1
  }

  async function markAllRead() {
    await client.patch('/notifications/read-all')
    notifications.value = notifications.value.map((item) => ({ ...item, readFlag: 1 }))
  }

  function connect(token: string) {
    if (stompClient?.active) return
    stompClient = new Client({
      brokerURL: `${window.location.origin.replace('http', 'ws')}/ws/notifications`,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 3000,
    })
    stompClient.onConnect = () => {
      stompClient?.subscribe('/user/queue/notifications', (message) => {
        const payload = JSON.parse(message.body) as NotificationRecord
        notifications.value.unshift(payload)
      })
    }
    stompClient.activate()
  }

  function disconnect() {
    stompClient?.deactivate()
    stompClient = null
  }

  return {
    notifications,
    unreadCount,
    fetchNotifications,
    markRead,
    markAllRead,
    connect,
    disconnect,
  }
})
