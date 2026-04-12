<script setup lang="ts">
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { useRoute, useRouter } from 'vue-router'
import client from '../api/client'
import ResumeExportDialog from '../components/ResumeExportDialog.vue'
import ResumePreviewDocument from '../components/ResumePreviewDocument.vue'
import type {
  CreateSavedResumeRequest,
  CreateSavedResumeResponse,
  ResumeCustomFieldItem,
  ResumeDetail,
  ResumeEducation,
  ResumeExperience,
  ResumeExtraSectionItem,
  ResumeModuleConfigItem,
  ResumePayload,
  ResumeProject,
  SavedResumeDetail,
  UpdateSavedResumeRequest,
  UpdateSavedResumeResponse,
} from '../types'

type ModuleCode = 'basicInfo' | 'jobIntent' | 'education' | 'workExperience' | 'projectExperience' | 'internshipExperience' | 'campusExperience' | 'skills' | 'honors' | 'selfEvaluation' | 'hobbies' | 'customFields'

const route = useRoute()
const router = useRouter()

const MODULE_DEFINITIONS: ResumeModuleConfigItem[] = [
  { code: 'basicInfo', label: '基本信息', visible: true, order: 0 },
  { code: 'jobIntent', label: '求职意向', visible: true, order: 1 },
  { code: 'education', label: '教育背景', visible: true, order: 2 },
  { code: 'workExperience', label: '工作经历', visible: true, order: 3 },
  { code: 'projectExperience', label: '项目经历', visible: true, order: 4 },
  { code: 'internshipExperience', label: '实习经历', visible: false, order: 5 },
  { code: 'campusExperience', label: '校园经历', visible: false, order: 6 },
  { code: 'skills', label: '技能特长', visible: true, order: 7 },
  { code: 'honors', label: '荣誉证书', visible: false, order: 8 },
  { code: 'selfEvaluation', label: '自我评价', visible: true, order: 9 },
  { code: 'hobbies', label: '兴趣爱好', visible: false, order: 10 },
  { code: 'customFields', label: '自定义信息', visible: false, order: 11 },
]

const loading = ref(false)
const exportDialogVisible = ref(false)
const saveDialogVisible = ref(false)
const saveDialogSubmitting = ref(false)
const savedResumeName = ref('')
const editingSavedResumeId = ref<number | null>(null)
const editingSavedResumeName = ref('')
const skillOptions = ref<string[]>([])
const missingItems = ref<string[]>([])
const completenessScore = ref(0)
const activeModule = ref<ModuleCode>('basicInfo')
const currentYear = dayjs().year()
const experienceOptions = [
  { label: '应届生', value: 0 },
  { label: '1年经验', value: 1 },
  { label: '2年经验', value: 2 },
  { label: '3年经验', value: 3 },
  { label: '4年经验', value: 4 },
  { label: '5年经验', value: 5 },
  { label: '6年经验', value: 6 },
  { label: '7年经验', value: 7 },
  { label: '8年经验', value: 8 },
  { label: '9年经验', value: 9 },
  { label: '10年及以上', value: 10 },
]
const educationDegreeOptions = [
  '不填',
  '初中',
  '高中',
  '中专',
  '大专',
  '本科',
  '硕士研究生',
  '博士研究生',
]

const form = reactive<ResumePayload>({
  templateCode: 'classic',
  fullName: '',
  gender: '',
  age: undefined,
  birthDate: '',
  displayAge: false,
  phone: '',
  email: '',
  city: '',
  summary: '',
  expectedCategory: '',
  expectedSalaryMin: undefined,
  expectedSalaryMax: undefined,
  highestEducation: '',
  yearsOfExperience: undefined,
  moduleConfig: [],
  educations: [],
  experiences: [],
  projects: [],
  internships: [],
  campusExperiences: [],
  honors: [],
  hobbies: [],
  customFields: [],
  skills: [],
})

const sortedModules = computed(() => [...form.moduleConfig].sort((a, b) => (a.order ?? 0) - (b.order ?? 0)))
const visibleModules = computed(() => sortedModules.value.filter((item) => item.visible))
const activeModuleConfig = computed(() => sortedModules.value.find((item) => item.code === activeModule.value) ?? sortedModules.value[0])
const activeModuleVisible = computed(() => Boolean(activeModuleConfig.value?.visible))
const basicInfoMissingFields = computed(() => {
  const missing: string[] = []
  if (!form.fullName?.trim()) missing.push('姓名')
  if (!form.gender?.trim()) missing.push('性别')
  if (!form.birthDate?.trim()) missing.push('出生年月')
  if (!form.phone?.trim()) missing.push('联系电话')
  if (!form.email?.trim()) missing.push('联系邮箱')
  if (!form.city?.trim()) missing.push('籍贯 / 城市')
  if (form.yearsOfExperience == null) missing.push('工作年限')
  return missing
})
const previewResumeDetail = computed<ResumeDetail>(() => ({
  resume: {
    id: form.id,
    templateCode: form.templateCode,
    fullName: form.fullName,
    gender: form.gender,
    age: form.age,
    birthDate: form.birthDate,
    displayAge: form.displayAge,
    phone: form.phone,
    email: form.email,
    city: form.city,
    summary: form.summary,
    expectedCategory: form.expectedCategory,
    expectedSalaryMin: form.expectedSalaryMin,
    expectedSalaryMax: form.expectedSalaryMax,
    highestEducation: form.highestEducation,
    yearsOfExperience: form.yearsOfExperience,
    completenessScore: completenessScore.value,
  },
  moduleConfig: form.moduleConfig.map((item) => ({ ...item })),
  educations: form.educations.map((item) => ({ ...item })),
  experiences: form.experiences.map((item) => ({ ...item })),
  projects: form.projects.map((item) => ({ ...item })),
  internships: form.internships.map((item) => ({ ...item })),
  campusExperiences: form.campusExperiences.map((item) => ({ ...item })),
  honors: form.honors.map((item) => ({ ...item })),
  hobbies: form.hobbies.map((item) => ({ ...item })),
  customFields: form.customFields.map((item) => ({ ...item })),
  skills: [...form.skills],
  missingItems: [...missingItems.value],
  completenessScore: completenessScore.value,
}))
const exportFilename = computed(() => `${(form.fullName?.trim() || '简历').replace(/[\\/:*?"<>|]/g, '-')}.pdf`)
const isEditingSavedResume = computed(() => editingSavedResumeId.value != null)
const editorStatusText = computed(() => (
  isEditingSavedResume.value
    ? `正在编辑：${editingSavedResumeName.value || '已保存简历'}`
    : '当前正在编辑主草稿'
))
const saveDialogTitle = computed(() => (isEditingSavedResume.value ? '更新简历' : '保存简历'))
const saveDialogHint = computed(() => (
  isEditingSavedResume.value
    ? '更新后会同步覆盖这份已保存简历，并更新当前草稿内容。'
    : '保存后会同步更新当前草稿，并在“我的简历”中新增一份命名简历。'
))
const saveDialogConfirmText = computed(() => (isEditingSavedResume.value ? '确认更新' : '确认保存'))

function parseSavedResumeId(value: unknown) {
  const rawValue = Array.isArray(value) ? value[0] : value
  const parsed = Number(rawValue)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null
}

function resetEditingSavedResumeContext() {
  editingSavedResumeId.value = null
  editingSavedResumeName.value = ''
}

async function fetchCurrentDraftResume() {
  const response = await client.get('/jobseeker/resume')
  hydrateResume(response.data as ResumeDetail)
  resetEditingSavedResumeContext()
}

async function fetchSavedResumeForEditing(savedResumeId: number) {
  const response = await client.get(`/jobseeker/saved-resumes/${savedResumeId}`)
  const detail = response.data as SavedResumeDetail
  hydrateResume(detail.resumeDetail)
  editingSavedResumeId.value = detail.id
  editingSavedResumeName.value = detail.name
  savedResumeName.value = detail.name
}

async function initializeResumeStudio() {
  loading.value = true
  try {
    const isEditMode = route.query.mode === 'edit'
    const savedResumeId = parseSavedResumeId(route.query.savedResumeId)

    if (isEditMode && savedResumeId != null) {
      try {
        await fetchSavedResumeForEditing(savedResumeId)
        return
      } catch (error) {
        ElMessage.error(
          String(
            (error as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message
              || (error as { message?: string })?.message
              || '已保存简历不存在',
          ),
        )
        await router.replace({ path: '/resume' })
      }
    }

    await fetchCurrentDraftResume()
  } finally {
    loading.value = false
  }
}

function hydrateResume(payload: ResumeDetail) {
  Object.assign(form, payload.resume, {
    birthDate: normalizeBirthDate(payload.resume.birthDate),
    moduleConfig: normalizeModuleConfig(payload.moduleConfig),
    educations: normalizeList(payload.educations),
    experiences: normalizeList(payload.experiences),
    projects: normalizeList(payload.projects),
    internships: normalizeList(payload.internships),
    campusExperiences: normalizeList(payload.campusExperiences),
    honors: normalizeList(payload.honors),
    hobbies: normalizeList(payload.hobbies),
    customFields: normalizeList(payload.customFields),
    skills: payload.skills ?? [],
  })
  missingItems.value = payload.missingItems
  completenessScore.value = payload.completenessScore
  ensureActiveModule()
}

function saveResume() {
  if (basicInfoMissingFields.value.length) {
    ElMessage.warning(`请先完善基本信息：${basicInfoMissingFields.value.join('、')}`)
    activeModule.value = 'basicInfo'
    return
  }
  savedResumeName.value = editingSavedResumeName.value || ''
  saveDialogVisible.value = true
}

function resetSaveResumeDialog() {
  savedResumeName.value = ''
  saveDialogSubmitting.value = false
}

async function submitSavedResume() {
  const name = savedResumeName.value.trim()
  if (!name) {
    ElMessage.warning('请输入简历名称')
    return
  }
  saveDialogSubmitting.value = true
  try {
    const updatingExisting = isEditingSavedResume.value && editingSavedResumeId.value != null
    const payload = {
      name,
      draft: buildPayload(),
    }
    const response = updatingExisting
      ? await client.put(`/jobseeker/saved-resumes/${editingSavedResumeId.value}`, payload as UpdateSavedResumeRequest)
      : await client.post('/jobseeker/saved-resumes', payload as CreateSavedResumeRequest)
    const data = response.data as CreateSavedResumeResponse | UpdateSavedResumeResponse
    hydrateResume(data.currentDraft)
    if (updatingExisting) {
      editingSavedResumeId.value = data.savedResume.id
      editingSavedResumeName.value = data.savedResume.name
    }
    saveDialogVisible.value = false
    ElMessage.success(`简历“${data.savedResume.name}”${updatingExisting ? '已更新' : '已保存'}`)
  } catch (error) {
    ElMessage.error(
      String(
        (error as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message
          || (error as { message?: string })?.message
          || '简历保存失败',
      ),
    )
  } finally {
    saveDialogSubmitting.value = false
  }
}

async function searchSkills(keyword: string) {
  const response = await client.get('/skills/suggest', { params: { keyword } })
  skillOptions.value = response.data as string[]
}

function selectModule(code: string) { activeModule.value = code as ModuleCode }
function ensureActiveModule() {
  const current = sortedModules.value.find((item) => item.code === activeModule.value)
  if (!current?.visible) activeModule.value = (visibleModules.value[0]?.code ?? sortedModules.value[0]?.code ?? 'basicInfo') as ModuleCode
}
function assignSortOrders<T extends { sortOrder?: number }>(items: T[]) { items.forEach((item, index) => { item.sortOrder = index }) }
function assignModuleOrders(items: ResumeModuleConfigItem[]) { items.forEach((item, index) => { item.order = index }) }
function normalizeList<T extends { sortOrder?: number }>(items?: T[]) { const normalized = [...(items ?? [])].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0)); assignSortOrders(normalized); return normalized }
function normalizeModuleConfig(items?: ResumeModuleConfigItem[]) {
  const incoming = new Map((items ?? []).map((item) => [item.code, item]))
  const normalized = MODULE_DEFINITIONS.map((item) => ({
    ...item,
    visible: item.code === 'basicInfo' ? true : (incoming.get(item.code)?.visible ?? item.visible),
    order: incoming.get(item.code)?.order ?? item.order,
  }))
  normalized.sort((a, b) => a.order - b.order)
  assignModuleOrders(normalized)
  return normalized
}
function cloneDefaultModuleConfig() { return normalizeModuleConfig(MODULE_DEFINITIONS) }
function normalizeMonthValue(value?: string) {
  if (!value) return ''
  const normalized = dayjs(value)
  if (!normalized.isValid()) return ''
  return normalized.format('YYYY-MM-01')
}
function normalizeBirthDate(value?: string) {
  if (!value) return ''
  const normalized = dayjs(value)
  if (!normalized.isValid()) return ''
  return normalized.format('YYYY-MM-01')
}
function handleBirthDateChange(value?: string) {
  const normalized = normalizeBirthDate(value)
  if (!normalized) {
    form.birthDate = ''
    return
  }
  if (dayjs(normalized).year() > currentYear) {
    ElMessage.error(`出生年月不能大于${currentYear}年`)
    form.birthDate = ''
    return
  }
  form.birthDate = normalized
}
function handleResumeMonthChange(item: { startDate?: string; endDate?: string }, field: 'startDate' | 'endDate', value?: string) {
  const normalized = normalizeMonthValue(value)
  if (!normalized) {
    item[field] = ''
    return
  }
  if (dayjs(`${normalized}-01`).year() > currentYear) {
    ElMessage.error(`时间年份不能大于${currentYear}年`)
    item[field] = ''
    return
  }
  item[field] = normalized
}
function normalizeResumeMonthSelection(item: { startDate?: string; endDate?: string }, field: 'startDate' | 'endDate') {
  handleResumeMonthChange(item, field, item[field])
}
function isCurrentEndDate(item: { current?: boolean; endDate?: string }) {
  return Boolean(item.current) || item.endDate === '至今'
}
function handleCurrentEndDateChange(item: { current?: boolean; endDate?: string }, checked: boolean | string | number) {
  item.current = Boolean(checked)
  if (item.current) {
    item.endDate = ''
  }
}
function createEducation(): ResumeEducation { return { schoolName: '', major: '', degree: '', startDate: '', endDate: '', current: false, description: '', sortOrder: 0 } }
function createExperience(): ResumeExperience { return { companyName: '', jobTitle: '', startDate: '', endDate: '', current: false, description: '', sortOrder: 0 } }
function createProject(): ResumeProject { return { projectName: '', roleName: '', startDate: '', endDate: '', current: false, description: '', sortOrder: 0 } }
function createExtraItem(): ResumeExtraSectionItem { return { title: '', subtitle: '', startDate: '', endDate: '', current: false, description: '', sortOrder: 0 } }
function createCustomField(): ResumeCustomFieldItem { return { key: '', value: '', sortOrder: 0 } }
function addEducation() { form.educations.push(createEducation()); assignSortOrders(form.educations) }
function addExperience() { form.experiences.push(createExperience()); assignSortOrders(form.experiences) }
function addProject() { form.projects.push(createProject()); assignSortOrders(form.projects) }
function addInternship() { form.internships.push(createExtraItem()); assignSortOrders(form.internships) }
function addCampusExperience() { form.campusExperiences.push(createExtraItem()); assignSortOrders(form.campusExperiences) }
function addHonor() { form.honors.push(createExtraItem()); assignSortOrders(form.honors) }
function addHobby() { form.hobbies.push(createExtraItem()); assignSortOrders(form.hobbies) }
function addCustomField() { form.customFields.push(createCustomField()); assignSortOrders(form.customFields) }
function removeListItem<T extends { sortOrder?: number }>(items: T[], index: number) { items.splice(index, 1); assignSortOrders(items) }
function moveListItem<T extends { sortOrder?: number }>(items: T[], index: number, delta: number) {
  const targetIndex = index + delta
  if (targetIndex < 0 || targetIndex >= items.length) return
  const [moved] = items.splice(index, 1)
  items.splice(targetIndex, 0, moved)
  assignSortOrders(items)
}
function clearModuleData(code: ModuleCode) {
  if (code === 'basicInfo') { form.fullName = ''; form.gender = ''; form.age = undefined; form.birthDate = ''; form.displayAge = false; form.phone = ''; form.email = ''; form.city = ''; form.yearsOfExperience = undefined; return }
  if (code === 'jobIntent') { form.expectedCategory = ''; form.expectedSalaryMin = undefined; form.expectedSalaryMax = undefined; return }
  if (code === 'selfEvaluation') { form.summary = ''; return }
  if (code === 'education') { form.educations = []; return }
  if (code === 'workExperience') { form.experiences = []; return }
  if (code === 'projectExperience') { form.projects = []; return }
  if (code === 'internshipExperience') { form.internships = []; return }
  if (code === 'campusExperience') { form.campusExperiences = []; return }
  if (code === 'skills') { form.skills = []; return }
  if (code === 'honors') { form.honors = []; return }
  if (code === 'hobbies') { form.hobbies = []; return }
  if (code === 'customFields') form.customFields = []
}
function handleModuleVisibilityChange(code: string, visible: boolean) {
  const target = form.moduleConfig.find((item) => item.code === code)
  if (!target) return
  if (code === 'basicInfo' && !visible) {
    target.visible = true
    ElMessage.warning('基本信息为必填模块，不能隐藏')
    return
  }
  target.visible = visible
  if (!visible) { clearModuleData(code as ModuleCode); if (activeModule.value === code) ensureActiveModule() }
}
function sortModuleConfig() { form.moduleConfig.sort((a, b) => (a.order ?? 0) - (b.order ?? 0)); assignModuleOrders(form.moduleConfig) }
function buildPayload(): ResumePayload {
  sortModuleConfig()
  assignSortOrders(form.educations)
  assignSortOrders(form.experiences)
  assignSortOrders(form.projects)
  assignSortOrders(form.internships)
  assignSortOrders(form.campusExperiences)
  assignSortOrders(form.honors)
  assignSortOrders(form.hobbies)
  assignSortOrders(form.customFields)
  return {
    ...form,
    moduleConfig: form.moduleConfig.map((item, index) => ({ ...item, order: index })),
    educations: form.educations.map((item, index) => ({ ...item, sortOrder: index })),
    experiences: form.experiences.map((item, index) => ({ ...item, sortOrder: index })),
    projects: form.projects.map((item, index) => ({ ...item, sortOrder: index })),
    internships: form.internships.map((item, index) => ({ ...item, sortOrder: index })),
    campusExperiences: form.campusExperiences.map((item, index) => ({ ...item, sortOrder: index })),
    honors: form.honors.map((item, index) => ({ ...item, sortOrder: index })),
    hobbies: form.hobbies.map((item, index) => ({ ...item, sortOrder: index })),
    customFields: form.customFields.map((item, index) => ({ ...item, sortOrder: index })),
    skills: [...form.skills],
  }
}

dayjs.locale('zh-cn')
form.moduleConfig = cloneDefaultModuleConfig()
watch(
  () => [route.query.mode, route.query.savedResumeId],
  () => {
    void initializeResumeStudio()
  },
  { immediate: true },
)
</script>

<template>
  <el-config-provider :locale="zhCn">
    <div class="resume-studio-page section-grid">
      <section class="studio-stage">
        <div class="paper-stage">
          <ResumePreviewDocument :detail="previewResumeDetail" mode="preview" />
        </div>
      </section>

      <section class="editor-shell">
        <div class="editor-toolbar">
          <div class="editor-toolbar__left">
            <span class="editor-status-chip">{{ editorStatusText }}</span>
          </div>
          <div class="editor-toolbar__right">
            <div class="completion-chip">{{ completenessScore }}% 完整</div>
            <el-button plain :disabled="loading" @click="exportDialogVisible = true">导出 PDF</el-button>
            <el-button type="primary" :loading="loading" @click="saveResume">保存简历</el-button>
          </div>
        </div>

      <div class="editor-tabs-wrap">
        <div class="editor-tabs">
          <template v-for="item in sortedModules" :key="`tab-${item.code}`">
            <button type="button" :class="['editor-tab', activeModule === item.code ? 'is-active' : '', !item.visible ? 'is-hidden' : '']" @click="selectModule(item.code)">
              <span :class="['editor-tab__dot', item.visible ? 'is-on' : 'is-off']"></span>
              <span>{{ item.label }}</span>
              <span v-if="item.code === 'basicInfo'" class="editor-tab__required">必填</span>
            </button>
            <div v-if="item.code === activeModule && activeModuleConfig" class="editor-tab-controls">
              <el-switch
                v-if="activeModuleConfig.code !== 'basicInfo'"
                :model-value="activeModuleConfig.visible"
                @change="(value: string | number | boolean) => handleModuleVisibilityChange(activeModuleConfig.code, Boolean(value))"
              />
            </div>
          </template>
        </div>
      </div>

      <section class="editor-card surface-card">
        <div class="editor-card__head">
          <div>
            <h3>{{ activeModuleConfig?.label || '模块编辑' }}</h3>
          </div>
        </div>

        <template v-if="activeModuleVisible">
          <div v-if="activeModule === 'basicInfo'" class="editor-form-grid">
            <el-form-item required label="您的姓名"><el-input v-model="form.fullName" placeholder="请输入姓名" /></el-form-item>
            <el-form-item required label="性别">
              <el-select v-model="form.gender" clearable placeholder="请选择">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
              </el-select>
            </el-form-item>
            <el-form-item required label="出生年月">
              <div class="birth-field">
                <el-date-picker
                  v-model="form.birthDate"
                  type="month"
                  format="YYYY年MM月"
                  value-format="YYYY-MM-01"
                  placeholder="请选择出生年月"
                  class="birth-picker"
                  @change="handleBirthDateChange"
                />
              </div>
            </el-form-item>
            <el-form-item required label="联系电话"><el-input v-model="form.phone" placeholder="请输入手机号" /></el-form-item>
            <el-form-item required label="联系邮箱"><el-input v-model="form.email" placeholder="请输入邮箱" /></el-form-item>
            <el-form-item required label="籍贯 / 城市"><el-input v-model="form.city" placeholder="请输入城市" /></el-form-item>
            <el-form-item required label="工作年限">
              <el-select v-model="form.yearsOfExperience" placeholder="请选择工作年限" clearable>
                <el-option v-for="item in experienceOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </div>

          <div v-else-if="activeModule === 'jobIntent'" class="editor-form-grid">
            <el-form-item label="求职意向"><el-input v-model="form.expectedCategory" /></el-form-item>
            <el-form-item label="期望薪资下限"><el-input-number v-model="form.expectedSalaryMin" :min="0" /></el-form-item>
            <el-form-item label="期望薪资上限"><el-input-number v-model="form.expectedSalaryMax" :min="0" /></el-form-item>
          </div>

          <template v-else-if="activeModule === 'education'">
            <div class="editor-list-head">
              <el-button type="primary" plain @click="addEducation">新增一条教育背景</el-button>
            </div>
            <el-empty v-if="!form.educations.length" description="还没有教育背景" />
            <article v-for="(item, index) in form.educations" :key="`edu-${index}`" class="editor-entry">
              <div class="editor-entry__body">
                <div class="editor-form-grid">
                  <el-form-item label="学校名称"><el-input v-model="item.schoolName" /></el-form-item>
                  <el-form-item label="所学专业"><el-input v-model="item.major" /></el-form-item>
                  <el-form-item label="学历">
                    <el-select v-model="item.degree" placeholder="请选择学历" clearable popper-class="degree-select-popper">
                      <el-option
                        v-for="degree in educationDegreeOptions"
                        :key="degree"
                        :label="degree"
                        :value="degree"
                      />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="时间" class="editor-time-field">
                    <div class="editor-time-range" :class="{ 'is-current': isCurrentEndDate(item) }">
                      <el-date-picker
                        v-model="item.startDate"
                        type="month"
                        format="YYYY年MM月"
                        value-format="YYYY-MM-01"
                        placeholder="开始时间"
                        class="editor-time-picker"
                        @change="() => normalizeResumeMonthSelection(item, 'startDate')"
                      />
                      <el-date-picker
                        v-if="!isCurrentEndDate(item)"
                        v-model="item.endDate"
                        type="month"
                        format="YYYY年MM月"
                        value-format="YYYY-MM-01"
                        placeholder="结束时间"
                        class="editor-time-picker"
                        @change="() => normalizeResumeMonthSelection(item, 'endDate')"
                      />
                      <el-checkbox :model-value="isCurrentEndDate(item)" @change="(value: string | number | boolean) => handleCurrentEndDateChange(item, value)">至今</el-checkbox>
                    </div>
                  </el-form-item>
                </div>
                <el-form-item label="详细描述">
                  <el-input v-model="item.description" type="textarea" :rows="5" />
                </el-form-item>
              </div>
              <div class="editor-entry__side">
                <el-button plain :disabled="index === 0" @click="moveListItem(form.educations, index, -1)">上移</el-button>
                <el-button plain :disabled="index === form.educations.length - 1" @click="moveListItem(form.educations, index, 1)">下移</el-button>
                <el-button plain type="danger" @click="removeListItem(form.educations, index)">删除</el-button>
              </div>
            </article>
          </template>

          <template v-else-if="activeModule === 'workExperience'">
            <div class="editor-list-head">
              <el-button type="primary" plain @click="addExperience">新增一条工作经历</el-button>
            </div>
            <el-empty v-if="!form.experiences.length" description="还没有工作经历" />
            <article v-for="(item, index) in form.experiences" :key="`exp-${index}`" class="editor-entry">
              <div class="editor-entry__body">
                <div class="editor-form-grid">
                  <el-form-item label="公司名称"><el-input v-model="item.companyName" /></el-form-item>
                  <el-form-item label="职位"><el-input v-model="item.jobTitle" /></el-form-item>
                  <el-form-item label="时间" class="editor-time-field">
                    <div class="editor-time-range" :class="{ 'is-current': isCurrentEndDate(item) }">
                      <el-date-picker
                        v-model="item.startDate"
                        type="month"
                        format="YYYY年MM月"
                        value-format="YYYY-MM-01"
                        placeholder="开始时间"
                        class="editor-time-picker"
                        @change="() => normalizeResumeMonthSelection(item, 'startDate')"
                      />
                      <el-date-picker
                        v-if="!isCurrentEndDate(item)"
                        v-model="item.endDate"
                        type="month"
                        format="YYYY年MM月"
                        value-format="YYYY-MM-01"
                        placeholder="结束时间"
                        class="editor-time-picker"
                        @change="() => normalizeResumeMonthSelection(item, 'endDate')"
                      />
                      <el-checkbox :model-value="isCurrentEndDate(item)" @change="(value: string | number | boolean) => handleCurrentEndDateChange(item, value)">至今</el-checkbox>
                    </div>
                  </el-form-item>
                </div>
                <el-form-item label="工作内容">
                  <el-input v-model="item.description" type="textarea" :rows="7" />
                </el-form-item>
              </div>
              <div class="editor-entry__side">
                <el-button plain :disabled="index === 0" @click="moveListItem(form.experiences, index, -1)">上移</el-button>
                <el-button plain :disabled="index === form.experiences.length - 1" @click="moveListItem(form.experiences, index, 1)">下移</el-button>
                <el-button plain type="danger" @click="removeListItem(form.experiences, index)">删除</el-button>
              </div>
            </article>
          </template>

          <template v-else-if="activeModule === 'projectExperience'">
            <div class="editor-list-head">
              <el-button type="primary" plain @click="addProject">新增一条项目经历</el-button>
            </div>
            <el-empty v-if="!form.projects.length" description="还没有项目经历" />
            <article v-for="(item, index) in form.projects" :key="`project-${index}`" class="editor-entry">
              <div class="editor-entry__body">
                <div class="editor-form-grid">
                  <el-form-item label="项目名称"><el-input v-model="item.projectName" /></el-form-item>
                  <el-form-item label="担任角色"><el-input v-model="item.roleName" /></el-form-item>
                  <el-form-item label="时间" class="editor-time-field">
                    <div class="editor-time-range" :class="{ 'is-current': isCurrentEndDate(item) }">
                      <el-date-picker
                        v-model="item.startDate"
                        type="month"
                        format="YYYY年MM月"
                        value-format="YYYY-MM-01"
                        placeholder="开始时间"
                        class="editor-time-picker"
                        @change="() => normalizeResumeMonthSelection(item, 'startDate')"
                      />
                      <el-date-picker
                        v-if="!isCurrentEndDate(item)"
                        v-model="item.endDate"
                        type="month"
                        format="YYYY年MM月"
                        value-format="YYYY-MM-01"
                        placeholder="结束时间"
                        class="editor-time-picker"
                        @change="() => normalizeResumeMonthSelection(item, 'endDate')"
                      />
                      <el-checkbox :model-value="isCurrentEndDate(item)" @change="(value: string | number | boolean) => handleCurrentEndDateChange(item, value)">至今</el-checkbox>
                    </div>
                  </el-form-item>
                </div>
                <el-form-item label="项目说明">
                  <el-input v-model="item.description" type="textarea" :rows="6" />
                </el-form-item>
              </div>
              <div class="editor-entry__side">
                <el-button plain :disabled="index === 0" @click="moveListItem(form.projects, index, -1)">上移</el-button>
                <el-button plain :disabled="index === form.projects.length - 1" @click="moveListItem(form.projects, index, 1)">下移</el-button>
                <el-button plain type="danger" @click="removeListItem(form.projects, index)">删除</el-button>
              </div>
            </article>
          </template>
          
          <template v-else-if="activeModule === 'internshipExperience'">
            <div class="editor-list-head">
              <el-button type="primary" plain @click="addInternship">新增一条实习经历</el-button>
            </div>
            <el-empty v-if="!form.internships.length" description="还没有实习经历" />
            <article v-for="(item, index) in form.internships" :key="`intern-${index}`" class="editor-entry">
              <div class="editor-entry__body">
                <div class="editor-form-grid">
                  <el-form-item label="公司 / 单位"><el-input v-model="item.title" /></el-form-item>
                  <el-form-item label="岗位 / 角色"><el-input v-model="item.subtitle" /></el-form-item>
                  <el-form-item label="时间" class="editor-time-field">
                    <div class="editor-time-range" :class="{ 'is-current': isCurrentEndDate(item) }">
                      <el-date-picker
                        v-model="item.startDate"
                        type="month"
                        format="YYYY年MM月"
                        value-format="YYYY-MM-01"
                        placeholder="开始时间"
                        class="editor-time-picker"
                        @change="() => normalizeResumeMonthSelection(item, 'startDate')"
                      />
                      <el-date-picker
                        v-if="!isCurrentEndDate(item)"
                        v-model="item.endDate"
                        type="month"
                        format="YYYY年MM月"
                        value-format="YYYY-MM-01"
                        placeholder="结束时间"
                        class="editor-time-picker"
                        @change="() => normalizeResumeMonthSelection(item, 'endDate')"
                      />
                      <el-checkbox :model-value="isCurrentEndDate(item)" @change="(value: string | number | boolean) => handleCurrentEndDateChange(item, value)">至今</el-checkbox>
                    </div>
                  </el-form-item>
                </div>
                <el-form-item label="实习内容"><el-input v-model="item.description" type="textarea" :rows="5" /></el-form-item>
              </div>
              <div class="editor-entry__side">
                <el-button plain :disabled="index === 0" @click="moveListItem(form.internships, index, -1)">上移</el-button>
                <el-button plain :disabled="index === form.internships.length - 1" @click="moveListItem(form.internships, index, 1)">下移</el-button>
                <el-button plain type="danger" @click="removeListItem(form.internships, index)">删除</el-button>
              </div>
            </article>
          </template>

          <template v-else-if="activeModule === 'campusExperience'">
            <div class="editor-list-head">
              <el-button type="primary" plain @click="addCampusExperience">新增一条校园经历</el-button>
            </div>
            <el-empty v-if="!form.campusExperiences.length" description="还没有校园经历" />
            <article v-for="(item, index) in form.campusExperiences" :key="`campus-${index}`" class="editor-entry">
              <div class="editor-entry__body">
                <div class="editor-form-grid">
                  <el-form-item label="组织 / 活动"><el-input v-model="item.title" /></el-form-item>
                  <el-form-item label="角色"><el-input v-model="item.subtitle" /></el-form-item>
                  <el-form-item label="时间" class="editor-time-field">
                    <div class="editor-time-range" :class="{ 'is-current': isCurrentEndDate(item) }">
                      <el-date-picker
                        v-model="item.startDate"
                        type="month"
                        format="YYYY年MM月"
                        value-format="YYYY-MM-01"
                        placeholder="开始时间"
                        class="editor-time-picker"
                        @change="() => normalizeResumeMonthSelection(item, 'startDate')"
                      />
                      <el-date-picker
                        v-if="!isCurrentEndDate(item)"
                        v-model="item.endDate"
                        type="month"
                        format="YYYY年MM月"
                        value-format="YYYY-MM-01"
                        placeholder="结束时间"
                        class="editor-time-picker"
                        @change="() => normalizeResumeMonthSelection(item, 'endDate')"
                      />
                      <el-checkbox :model-value="isCurrentEndDate(item)" @change="(value: string | number | boolean) => handleCurrentEndDateChange(item, value)">至今</el-checkbox>
                    </div>
                  </el-form-item>
                </div>
                <el-form-item label="内容描述"><el-input v-model="item.description" type="textarea" :rows="5" /></el-form-item>
              </div>
              <div class="editor-entry__side">
                <el-button plain :disabled="index === 0" @click="moveListItem(form.campusExperiences, index, -1)">上移</el-button>
                <el-button plain :disabled="index === form.campusExperiences.length - 1" @click="moveListItem(form.campusExperiences, index, 1)">下移</el-button>
                <el-button plain type="danger" @click="removeListItem(form.campusExperiences, index)">删除</el-button>
              </div>
            </article>
          </template>

          <template v-else-if="activeModule === 'skills'">
            <div class="skill-editor">
              <el-form-item label="技能特长">
                <el-select v-model="form.skills" multiple filterable remote allow-create default-first-option class="skill-select" :remote-method="searchSkills">
                  <el-option v-for="item in skillOptions" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
              <div class="skill-preview-strip">
                <span v-for="skill in form.skills" :key="skill" class="skill-tag">{{ skill }}</span>
                <span v-if="!form.skills.length" class="empty-inline">请输入或选择技能标签</span>
              </div>
            </div>
          </template>

          <template v-else-if="activeModule === 'honors'">
            <div class="editor-list-head">
              <el-button type="primary" plain @click="addHonor">新增一条荣誉证书</el-button>
            </div>
            <el-empty v-if="!form.honors.length" description="还没有荣誉证书" />
            <article v-for="(item, index) in form.honors" :key="`honor-${index}`" class="editor-entry">
              <div class="editor-entry__body">
                <div class="editor-form-grid">
                  <el-form-item label="荣誉 / 证书名称"><el-input v-model="item.title" /></el-form-item>
                  <el-form-item label="授予单位"><el-input v-model="item.subtitle" /></el-form-item>
                  <el-form-item label="获得日期">
                    <el-date-picker
                      v-model="item.startDate"
                      type="month"
                      format="YYYY年MM月"
                      value-format="YYYY-MM-01"
                      placeholder="获得日期"
                      class="editor-time-picker"
                      @change="() => normalizeResumeMonthSelection(item, 'startDate')"
                    />
                  </el-form-item>
                </div>
                <el-form-item label="补充说明"><el-input v-model="item.description" type="textarea" :rows="4" /></el-form-item>
              </div>
              <div class="editor-entry__side">
                <el-button plain :disabled="index === 0" @click="moveListItem(form.honors, index, -1)">上移</el-button>
                <el-button plain :disabled="index === form.honors.length - 1" @click="moveListItem(form.honors, index, 1)">下移</el-button>
                <el-button plain type="danger" @click="removeListItem(form.honors, index)">删除</el-button>
              </div>
            </article>
          </template>

          <template v-else-if="activeModule === 'selfEvaluation'">
            <el-form-item label="自我评价">
              <el-input v-model="form.summary" type="textarea" :rows="8" placeholder="建议从职业定位、能力结构、代表成果和个人优势四个角度进行概括。" />
            </el-form-item>
          </template>

          <template v-else-if="activeModule === 'hobbies'">
            <div class="editor-list-head">
              <el-button type="primary" plain @click="addHobby">新增一条兴趣爱好</el-button>
            </div>
            <el-empty v-if="!form.hobbies.length" description="还没有兴趣爱好" />
            <article v-for="(item, index) in form.hobbies" :key="`hobby-${index}`" class="editor-entry">
              <div class="editor-entry__body">
                <div class="editor-form-grid">
                  <el-form-item label="兴趣名称"><el-input v-model="item.title" /></el-form-item>
                  <el-form-item label="补充说明"><el-input v-model="item.description" /></el-form-item>
                </div>
              </div>
              <div class="editor-entry__side">
                <el-button plain :disabled="index === 0" @click="moveListItem(form.hobbies, index, -1)">上移</el-button>
                <el-button plain :disabled="index === form.hobbies.length - 1" @click="moveListItem(form.hobbies, index, 1)">下移</el-button>
                <el-button plain type="danger" @click="removeListItem(form.hobbies, index)">删除</el-button>
              </div>
            </article>
          </template>

          <template v-else-if="activeModule === 'customFields'">
            <div class="editor-list-head">
              <el-button type="primary" plain @click="addCustomField">新增一条自定义信息</el-button>
            </div>
            <el-empty v-if="!form.customFields.length" description="还没有自定义信息" />
            <article v-for="(item, index) in form.customFields" :key="`custom-${index}`" class="editor-entry">
              <div class="editor-entry__body">
                <div class="editor-form-grid">
                  <el-form-item label="信息名称"><el-input v-model="item.key" /></el-form-item>
                  <el-form-item label="信息内容"><el-input v-model="item.value" /></el-form-item>
                </div>
              </div>
              <div class="editor-entry__side">
                <el-button plain :disabled="index === 0" @click="moveListItem(form.customFields, index, -1)">上移</el-button>
                <el-button plain :disabled="index === form.customFields.length - 1" @click="moveListItem(form.customFields, index, 1)">下移</el-button>
                <el-button plain type="danger" @click="removeListItem(form.customFields, index)">删除</el-button>
              </div>
            </article>
          </template>
        </template>

        <div v-else class="module-hidden-state">
          <el-empty :description="''" />
          <el-button type="primary" @click="handleModuleVisibilityChange(activeModuleConfig?.code || activeModule, true)">打开当前模块</el-button>
        </div>
      </section>
    </section>
      <el-dialog
        v-model="saveDialogVisible"
        width="420px"
        :title="saveDialogTitle"
        destroy-on-close
        @closed="resetSaveResumeDialog"
      >
        <el-form label-position="top">
          <el-form-item label="简历名称">
            <el-input
              v-model.trim="savedResumeName"
              maxlength="40"
              show-word-limit
              placeholder="例如：简历一"
              @keyup.enter="submitSavedResume"
            />
          </el-form-item>
        </el-form>
        <p class="save-resume-hint">{{ saveDialogHint }}</p>
        <template #footer>
          <div class="save-resume-footer">
            <el-button @click="saveDialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="saveDialogSubmitting" @click="submitSavedResume">{{ saveDialogConfirmText }}</el-button>
          </div>
        </template>
      </el-dialog>
      <ResumeExportDialog
        v-model="exportDialogVisible"
        :loading="loading"
        :detail="previewResumeDetail"
        :filename="exportFilename"
        title="导出简历 PDF"
        subtitle="将按照当前简历内容自动分页，保证导出内容完整。"
      />
    </div>
  </el-config-provider>
</template>

<style scoped>
.resume-studio-page { gap: 22px; }
.studio-stage {
  display: block;
  padding: 12px 0;
  background: transparent;
  border: none;
  box-shadow: none;
}
.paper-stage {
  display: grid;
  gap: 0;
  justify-items: center;
  align-items: start;
  width: 100%;
  padding: 0;
}
.resume-paper {
  width: min(100%, 820px);
  aspect-ratio: 210 / 297;
  border-radius: 18px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  box-shadow: 0 20px 42px rgba(15, 23, 42, 0.08);
  overflow: hidden;
}
.resume-paper__inner { height: 100%; padding: 40px 52px 48px; }
.paper-header { display: flex; justify-content: center; gap: 24px; align-items: flex-start; margin-bottom: 36px; }
.paper-header__main { flex: 0 1 760px; text-align: center; }
.paper-header__main h1 { margin: 0; font-size: 28px; font-weight: 700; color: #111827; }
.paper-header__main p { margin: 12px 0 0; color: #374151; font-size: 14px; line-height: 1.7; }
.paper-section { margin-top: 34px; }
.paper-section__title {
  padding-bottom: 8px;
  border-bottom: 1px solid #2f2f2f;
  font-size: 17px;
  font-weight: 700;
  color: #111827;
}
.paper-entry { margin-top: 16px; }
.paper-entry__head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  font-size: 15px;
  color: #111827;
}
.paper-entry p, .paper-section p { margin: 8px 0 0; color: #374151; line-height: 1.85; white-space: pre-line; }
.paper-empty { color: #94a3b8; font-size: 14px; }
.paper-skill-list { margin-top: 14px; display: flex; flex-wrap: wrap; gap: 10px; }
.paper-skill-pill {
  padding: 6px 14px;
  border-radius: 999px;
  background: #eef6ff;
  color: #1e3a8a;
  font-size: 13px;
  font-weight: 600;
}
.paper-custom-list { margin-top: 14px; display: grid; gap: 8px; }
.paper-custom-row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  padding-bottom: 8px;
  border-bottom: 1px dashed #e5e7eb;
}
.editor-tab__dot { width: 10px; height: 10px; border-radius: 999px; display: inline-flex; }
.editor-tab__dot.is-on { background: #7dd3a7; }
.editor-tab__dot.is-off { background: #d1d5db; }
.editor-shell {
  padding: 20px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(24, 36, 43, 0.08);
  box-shadow: var(--shadow-soft);
}
.editor-toolbar__left { display: flex; align-items: center; }
.editor-status-chip {
  display: inline-flex;
  align-items: center;
  padding: 9px 14px;
  border-radius: 999px;
  background: rgba(14, 165, 233, 0.1);
  color: #0369a1;
  font-size: 13px;
  font-weight: 700;
}
.editor-toolbar { display: flex; justify-content: space-between; gap: 16px; align-items: center; }
.editor-toolbar__right { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; justify-content: flex-end; }
.save-resume-hint {
  margin: 0;
  color: var(--text-muted);
  line-height: 1.7;
}
.save-resume-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
.completion-chip {
  padding: 9px 14px;
  border-radius: 999px;
  background: rgba(34, 197, 94, 0.12);
  color: #2f8f46;
  font-size: 13px;
  font-weight: 700;
}
.editor-tabs-wrap {
  margin-top: 18px;
  border-bottom: 1px solid #eceef4;
  display: flex;
  justify-content: center;
}
.editor-tabs {
  max-width: 100%;
  padding: 10px 14px 0;
  display: flex;
  gap: 10px;
  align-items: center;
  overflow-x: auto;
}
.editor-tab {
  flex: 0 0 auto;
  padding: 0 0 11px;
  border: none;
  background: transparent;
  display: inline-flex;
  gap: 8px;
  align-items: center;
  color: #1f2937;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}
.editor-tab.is-active { color: #f97316; box-shadow: inset 0 -2px 0 #f97316; }
.editor-tab.is-hidden { color: #c4c8d0; }
.editor-tab__required {
  display: inline-flex;
  padding: 2px 6px;
  border-radius: 999px;
  background: rgba(249, 115, 22, 0.12);
  color: #f97316;
  font-size: 11px;
  font-weight: 700;
}
.editor-tab-controls {
  flex: 0 0 auto;
  display: inline-flex;
  gap: 8px;
  align-items: center;
  padding-bottom: 11px;
}
:deep(.editor-tab-controls .el-switch__label) {
  display: none;
}
.editor-card { margin-top: 18px; padding: 22px; }
.editor-card__head { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.editor-card__head h3 { margin: 0; font-size: 18px; }
.editor-form-grid { display: grid; gap: 14px; grid-template-columns: repeat(4, minmax(0, 1fr)); }
.birth-field {
  display: flex;
  gap: 12px;
  align-items: center;
}
.birth-picker {
  flex: 1;
}
.editor-time-field {
  grid-column: span 2;
}
.editor-time-range {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: nowrap;
}
.editor-time-range.is-current .editor-time-picker {
  width: 172px;
}
.editor-time-picker {
  width: 172px;
}
:deep(.degree-select-popper) {
  width: 220px !important;
}
.editor-list-head {
  display: flex;
  justify-content: flex-end;
  gap: 14px;
  align-items: center;
  margin-bottom: 16px;
}
.editor-entry {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 112px;
  gap: 18px;
  padding: 18px 0;
  border-top: 1px dashed #f0c8a5;
}
.editor-entry:first-of-type { border-top: none; }
.editor-entry__body { min-width: 0; }
.editor-entry__side { display: grid; gap: 12px; align-content: start; }
.module-hidden-state { display: grid; justify-items: center; gap: 12px; padding: 22px 0 6px; }
.skill-editor { display: grid; gap: 16px; }
.skill-select { width: 100%; }
.skill-preview-strip { display: flex; flex-wrap: wrap; gap: 10px; }
.skill-tag {
  padding: 7px 14px;
  border-radius: 999px;
  background: rgba(20, 184, 166, 0.12);
  color: #0f766e;
  font-size: 13px;
  font-weight: 700;
}
.empty-inline { color: var(--text-muted); font-size: 13px; }
@media (max-width: 980px) {
  .studio-stage { padding: 8px 0; }
  .resume-paper { width: min(100%, 700px); }
  .resume-paper__inner { padding: 28px 28px 36px; }
  .editor-form-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .editor-entry { grid-template-columns: 1fr; }
  .editor-entry__side { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .birth-field { flex-wrap: wrap; }
  .editor-time-field { grid-column: span 2; }
  .editor-time-range { flex-wrap: wrap; }
  .editor-time-picker { width: 100%; }
}
@media (max-width: 760px) {
  .studio-stage { padding: 0; }
  .paper-header,
  .editor-toolbar,
  .editor-card__head,
  .editor-list-head { flex-direction: column; align-items: flex-start; }
  .paper-header__main { text-align: left; }
  .resume-paper {
    width: 100%;
    aspect-ratio: auto;
  }
  .editor-form-grid { grid-template-columns: 1fr; }
  .editor-time-field { grid-column: span 1; }
  .editor-tabs-wrap { justify-content: flex-start; }
  .editor-tabs { padding-left: 0; padding-right: 0; }
}
</style>
