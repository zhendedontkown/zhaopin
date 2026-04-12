<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import ResumePreviewDocument from './ResumePreviewDocument.vue'
import { exportResumeElementToPdf } from '../composables/useResumePdfExport'
import type { ResumeDetail } from '../types'

const props = withDefaults(defineProps<{
  modelValue: boolean
  loading?: boolean
  error?: string
  detail: ResumeDetail | null
  title: string
  subtitle?: string
  filename?: string
  currentNotice?: string
  mode?: 'export' | 'preview'
}>(), {
  filename: '',
  mode: 'export',
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  closed: []
}>()

const exporting = ref(false)
const documentRef = ref<{ getRootElement?: () => HTMLElement | null } | null>(null)

const previewMode = computed(() => props.mode === 'preview')
const canExport = computed(() => (
  !previewMode.value
  && Boolean(props.detail)
  && Boolean(props.filename)
  && !props.loading
  && !props.error
  && !exporting.value
))

async function handleExport() {
  if (!canExport.value) return

  const root = documentRef.value?.getRootElement?.()
  if (!root) {
    ElMessage.error('PDF 导出失败，简历预览尚未完成渲染。')
    return
  }

  exporting.value = true
  try {
    await exportResumeElementToPdf(root, props.filename)
    ElMessage.success('PDF 已开始下载')
  } catch (error) {
    const message = error instanceof Error ? error.message : 'PDF 导出失败，请稍后重试。'
    ElMessage.error(message)
  } finally {
    exporting.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    class="resume-export-dialog"
    width="920px"
    align-center
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
    @closed="emit('closed')"
  >
    <template #header>
      <div class="resume-export-head">
        <h3>{{ title }}</h3>
        <p v-if="subtitle">{{ subtitle }}</p>
      </div>
    </template>

    <div v-loading="loading" class="resume-export-body">
      <template v-if="detail">
        <el-alert
          v-if="currentNotice"
          class="resume-export-alert"
          type="warning"
          :closable="false"
          show-icon
          :title="currentNotice"
        />

        <div class="resume-export-stage">
          <ResumePreviewDocument
            ref="documentRef"
            :detail="detail"
            mode="export"
          />
        </div>
      </template>

      <el-result
        v-else-if="error"
        icon="error"
        title="无法加载简历"
        :sub-title="error"
      />
    </div>

    <template v-if="!previewMode" #footer>
      <div class="resume-export-footer">
        <el-button @click="emit('update:modelValue', false)">取消</el-button>
        <el-button
          type="primary"
          :loading="exporting"
          :disabled="!canExport"
          @click="handleExport"
        >
          确认导出
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.resume-export-head h3 {
  margin: 0;
  font-size: 20px;
  color: var(--text-strong);
}

.resume-export-head p {
  margin: 8px 0 0;
  color: var(--text-muted);
  line-height: 1.6;
}

.resume-export-body {
  min-height: 280px;
}

.resume-export-alert {
  margin-bottom: 16px;
}

.resume-export-stage {
  display: grid;
  justify-items: center;
  padding: 4px 0 8px;
}

.resume-export-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

:deep(.resume-export-dialog .el-dialog) {
  border-radius: 24px;
  overflow: hidden;
}

:deep(.resume-export-dialog .el-dialog__body) {
  padding: 20px 24px 12px;
  background: #f8fafc;
}
</style>
