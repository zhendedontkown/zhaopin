<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { ResumeDetail, ResumeExtraSectionItem, ResumeModuleConfigItem } from '../types'

type ModuleCode =
  | 'basicInfo'
  | 'jobIntent'
  | 'education'
  | 'workExperience'
  | 'projectExperience'
  | 'internshipExperience'
  | 'campusExperience'
  | 'skills'
  | 'honors'
  | 'selfEvaluation'
  | 'hobbies'
  | 'customFields'

type SectionKind = 'entries' | 'custom' | 'pills' | 'text'

interface SectionItem {
  key: string
  title?: string
  period?: string
  description?: string
  label?: string
  value?: string
  text?: string
}

interface SectionSpec {
  code: ModuleCode
  label: string
  kind: SectionKind
  items: SectionItem[]
  splittable: boolean
}

interface SectionMetrics {
  outerHeight: number
  marginTop: number
  firstItemTop: number
  itemTops: number[]
  itemBottoms: number[]
}

interface PageFragment {
  key: string
  section: SectionSpec
  itemStart: number
  itemEnd: number
}

interface PageSpec {
  key: string
  showHeader: boolean
  fragments: PageFragment[]
}

const props = withDefaults(defineProps<{
  detail: ResumeDetail
  mode?: 'preview' | 'export'
}>(), {
  mode: 'preview',
})

const hostRef = ref<HTMLElement | null>(null)
const rootRef = ref<HTMLElement | null>(null)
const templateInnerRef = ref<HTMLElement | null>(null)
const measureRef = ref<HTMLElement | null>(null)
const pages = ref<PageSpec[]>([])
const paginationReady = ref(false)

let resizeObserver: ResizeObserver | null = null
let paginationFrameId: number | null = null

const resume = computed(() => props.detail.resume)
const isExportMode = computed(() => props.mode === 'export')

const sortedModules = computed(() =>
  [...(props.detail.moduleConfig ?? [])].sort((a, b) => (a.order ?? 0) - (b.order ?? 0)),
)

const visibleModules = computed(() => sortedModules.value.filter((item) => item.visible))
const contentModules = computed(() =>
  visibleModules.value.filter((item) => item.code !== 'basicInfo' && item.code !== 'jobIntent'),
)

const previewName = computed(() => resume.value.fullName || '全民简历')

const previewExperienceText = computed(() => {
  if (resume.value.yearsOfExperience == null) return ''
  if (resume.value.yearsOfExperience === 0) return '应届生'
  if (resume.value.yearsOfExperience >= 10) return '10年及以上'
  return `${resume.value.yearsOfExperience}年经验`
})

const previewBasicMeta = computed(() =>
  [resume.value.gender, formatBirthMonth(resume.value.birthDate), resume.value.city, previewExperienceText.value]
    .filter(Boolean)
    .join(' · ') || '性别 · 出生年月 · 城市 · 工作经验',
)

const previewContactMeta = computed(() =>
  [resume.value.phone, resume.value.email].filter(Boolean).join(' · ') || '手机号 · 邮箱',
)

const previewIntentMeta = computed(() => {
  const salary =
    resume.value.expectedSalaryMin != null || resume.value.expectedSalaryMax != null
      ? `${resume.value.expectedSalaryMin ?? '面议'}-${resume.value.expectedSalaryMax ?? '面议'} 元/月`
      : '薪资面议'

  return [
    resume.value.expectedCategory || '目标岗位待补充',
    resume.value.city || '目标城市待补充',
    salary,
  ].join(' · ')
})

const sectionSpecs = computed<SectionSpec[]>(() =>
  contentModules.value
    .map((module) => buildSectionSpec(module))
    .filter((section): section is SectionSpec => section !== null),
)

function formatPeriodMonth(value?: string) {
  return value ? value.slice(0, 7) : ''
}

function formatBirthMonth(value?: string) {
  const month = formatPeriodMonth(value)
  if (!month) return ''

  const [year, monthValue] = month.split('-')
  return `${year}年${monthValue}月`
}

function sectionPeriod(startDate?: string, endDate?: string, current?: boolean) {
  if (!startDate && !endDate && !current) return '时间待补充'
  return `${formatPeriodMonth(startDate) || '开始时间'} ~ ${current ? '至今' : formatPeriodMonth(endDate) || '结束时间'}`
}

function renderExtraTitle(
  item: ResumeExtraSectionItem,
  fallbackTitle: string,
  fallbackSubtitle: string,
) {
  return `${item.title || fallbackTitle} - ${item.subtitle || fallbackSubtitle}`
}

function createEntrySection(
  code: ModuleCode,
  label: string,
  items: SectionItem[],
  emptyText: string,
): SectionSpec {
  return {
    code,
    label,
    kind: 'entries',
    splittable: true,
    items: items.length ? items : [{ key: `${code}-empty`, text: emptyText }],
  }
}

function buildSectionSpec(module: ResumeModuleConfigItem): SectionSpec | null {
  const code = module.code as ModuleCode

  switch (code) {
    case 'education':
      return createEntrySection(
        code,
        module.label,
        props.detail.educations.map((item, index) => ({
          key: `education-${index}`,
          title: `${item.schoolName || '学校名称'} - ${item.major || '专业'} - ${item.degree || '学历'}`,
          period: sectionPeriod(item.startDate, item.endDate, item.current),
          description: item.description || '补充课程、成绩、项目或校园成果。',
        })),
        '教育背景暂未填写',
      )
    case 'workExperience':
      return createEntrySection(
        code,
        module.label,
        props.detail.experiences.map((item, index) => ({
          key: `experience-${index}`,
          title: `${item.companyName || '公司名称'} - ${item.jobTitle || '岗位名称'}`,
          period: sectionPeriod(item.startDate, item.endDate, item.current),
          description: item.description || '补充职责、动作与结果。',
        })),
        '工作经历暂未填写',
      )
    case 'projectExperience':
      return createEntrySection(
        code,
        module.label,
        props.detail.projects.map((item, index) => ({
          key: `project-${index}`,
          title: `${item.projectName || '项目名称'} - ${item.roleName || '担任角色'}`,
          period: sectionPeriod(item.startDate, item.endDate, item.current),
          description: item.description || '补充背景、职责与项目成果。',
        })),
        '项目经历暂未填写',
      )
    case 'internshipExperience':
      return createEntrySection(
        code,
        module.label,
        props.detail.internships.map((item, index) => ({
          key: `internship-${index}`,
          title: renderExtraTitle(item, '公司 / 单位', '岗位 / 角色'),
          period: sectionPeriod(item.startDate, item.endDate, item.current),
          description: item.description || '补充实习内容与成果。',
        })),
        '实习经历暂未填写',
      )
    case 'campusExperience':
      return createEntrySection(
        code,
        module.label,
        props.detail.campusExperiences.map((item, index) => ({
          key: `campus-${index}`,
          title: renderExtraTitle(item, '组织 / 活动', '角色'),
          period: sectionPeriod(item.startDate, item.endDate, item.current),
          description: item.description || '补充活动内容与收获。',
        })),
        '校园经历暂未填写',
      )
    case 'honors':
      return createEntrySection(
        code,
        module.label,
        props.detail.honors.map((item, index) => ({
          key: `honor-${index}`,
          title: renderExtraTitle(item, '荣誉名称', '授予单位'),
          period: formatPeriodMonth(item.startDate) || '日期待补充',
          description: item.description || '补充荣誉背景与说明。',
        })),
        '荣誉证书暂未填写',
      )
    case 'skills':
      return {
        code,
        label: module.label,
        kind: 'pills',
        splittable: false,
        items: props.detail.skills.length
          ? props.detail.skills.map((skill, index) => ({
            key: `skill-${index}`,
            label: skill,
          }))
          : [{ key: 'skill-empty', text: '技能特长暂未填写' }],
      }
    case 'selfEvaluation':
      return {
        code,
        label: module.label,
        kind: 'text',
        splittable: false,
        items: [{
          key: 'self-evaluation',
          text: resume.value.summary || '请补充你的职业定位、能力结构和代表性优势。',
        }],
      }
    case 'hobbies':
      return {
        code,
        label: module.label,
        kind: 'pills',
        splittable: false,
        items: props.detail.hobbies.length
          ? props.detail.hobbies.map((item, index) => ({
            key: `hobby-${index}`,
            label: item.title || '兴趣名称',
          }))
          : [{ key: 'hobby-empty', text: '兴趣爱好暂未填写' }],
      }
    case 'customFields':
      return {
        code,
        label: module.label,
        kind: 'custom',
        splittable: true,
        items: props.detail.customFields.length
          ? props.detail.customFields.map((item, index) => ({
            key: `custom-${index}`,
            label: item.key || '信息名称',
            value: item.value || '信息内容',
          }))
          : [{ key: 'custom-empty', text: '自定义信息暂未填写' }],
      }
    default:
      return null
  }
}

function outerHeight(element: HTMLElement) {
  const styles = window.getComputedStyle(element)
  const marginTop = Number.parseFloat(styles.marginTop) || 0
  const marginBottom = Number.parseFloat(styles.marginBottom) || 0
  return element.offsetHeight + marginTop + marginBottom
}

function getAvailablePageHeight() {
  const inner = templateInnerRef.value
  if (!inner) return 0

  const styles = window.getComputedStyle(inner)
  const paddingTop = Number.parseFloat(styles.paddingTop) || 0
  const paddingBottom = Number.parseFloat(styles.paddingBottom) || 0
  return inner.clientHeight - paddingTop - paddingBottom
}

function measureSectionMetrics() {
  const measureRoot = measureRef.value
  if (!measureRoot) return new Map<string, SectionMetrics>()

  const metrics = new Map<string, SectionMetrics>()
  const sectionElements = Array.from(measureRoot.querySelectorAll<HTMLElement>('[data-section-code]'))

  sectionElements.forEach((sectionElement) => {
    const code = sectionElement.dataset.sectionCode
    if (!code) return

    const styles = window.getComputedStyle(sectionElement)
    const marginTop = Number.parseFloat(styles.marginTop) || 0
    const itemElements = Array.from(sectionElement.querySelectorAll<HTMLElement>('[data-role="section-item"]'))
    const sectionRect = sectionElement.getBoundingClientRect()
    const itemRects = itemElements.map((item) => item.getBoundingClientRect())

    metrics.set(code, {
      outerHeight: outerHeight(sectionElement),
      marginTop,
      firstItemTop: itemRects[0] ? itemRects[0].top - sectionRect.top : 0,
      itemTops: itemRects.map((rect) => rect.top - sectionRect.top),
      itemBottoms: itemRects.map((rect) => rect.bottom - sectionRect.top),
    })
  })

  return metrics
}

function getFragmentHeight(metrics: SectionMetrics, start: number, end: number) {
  if (end <= start || !metrics.itemTops.length || !metrics.itemBottoms.length) {
    return metrics.outerHeight
  }

  return metrics.marginTop + metrics.firstItemTop + (metrics.itemBottoms[end - 1] - metrics.itemTops[start])
}

function buildPages(availableHeight: number, headerHeight: number, metricsMap: Map<string, SectionMetrics>) {
  const builtPages: PageSpec[] = []
  let pageIndex = 0
  let currentPage: PageSpec = {
    key: `resume-page-${pageIndex}`,
    showHeader: true,
    fragments: [],
  }
  let remainingHeight = Math.max(0, availableHeight - headerHeight)

  const pushCurrentPage = () => {
    if (!currentPage.showHeader && currentPage.fragments.length === 0) return

    builtPages.push({
      ...currentPage,
      key: `resume-page-${builtPages.length}`,
    })

    pageIndex += 1
    currentPage = {
      key: `resume-page-${pageIndex}`,
      showHeader: false,
      fragments: [],
    }
    remainingHeight = availableHeight
  }

  sectionSpecs.value.forEach((section) => {
    const metrics = metricsMap.get(section.code)
    if (!metrics) return

    if (!section.splittable || section.items.length <= 1) {
      const sectionHeight = metrics.outerHeight
      const shouldMoveToNextPage =
        sectionHeight <= availableHeight &&
        sectionHeight > remainingHeight &&
        (currentPage.showHeader || currentPage.fragments.length > 0)

      if (shouldMoveToNextPage) {
        pushCurrentPage()
      }

      currentPage.fragments.push({
        key: `${currentPage.key}-${section.code}-0-${section.items.length}`,
        section,
        itemStart: 0,
        itemEnd: section.items.length,
      })

      remainingHeight = Math.max(0, remainingHeight - sectionHeight)
      return
    }

    let start = 0

    while (start < section.items.length) {
      const firstItemHeight = getFragmentHeight(metrics, start, start + 1)
      const shouldMoveToNextPage =
        firstItemHeight <= availableHeight &&
        firstItemHeight > remainingHeight &&
        (currentPage.showHeader || currentPage.fragments.length > 0)

      if (shouldMoveToNextPage) {
        pushCurrentPage()
        continue
      }

      let end = start + 1

      while (end < section.items.length) {
        const nextHeight = getFragmentHeight(metrics, start, end + 1)
        if (nextHeight > remainingHeight || nextHeight > availableHeight) {
          break
        }
        end += 1
      }

      currentPage.fragments.push({
        key: `${currentPage.key}-${section.code}-${start}-${end}`,
        section,
        itemStart: start,
        itemEnd: end,
      })

      const fragmentHeight = getFragmentHeight(metrics, start, end)
      remainingHeight = Math.max(0, remainingHeight - fragmentHeight)
      start = end

      if (start < section.items.length) {
        pushCurrentPage()
      }
    }
  })

  if (currentPage.showHeader || currentPage.fragments.length > 0) {
    builtPages.push({
      ...currentPage,
      key: `resume-page-${builtPages.length}`,
    })
  }

  return builtPages
}

async function paginateDocument() {
  await nextTick()

  const availableHeight = getAvailablePageHeight()
  const measureRoot = measureRef.value
  const headerElement = measureRoot?.querySelector<HTMLElement>('[data-role="resume-header"]')

  if (!availableHeight || !measureRoot || !headerElement) {
    pages.value = []
    paginationReady.value = false
    return
  }

  const metricsMap = measureSectionMetrics()
  const builtPages = buildPages(availableHeight, outerHeight(headerElement), metricsMap)

  pages.value = builtPages.length
    ? builtPages
    : [{
      key: 'resume-page-0',
      showHeader: true,
      fragments: [],
    }]

  paginationReady.value = true
}

function schedulePagination() {
  paginationReady.value = false

  if (paginationFrameId != null) {
    window.cancelAnimationFrame(paginationFrameId)
  }

  paginationFrameId = window.requestAnimationFrame(() => {
    paginationFrameId = null
    void paginateDocument()
  })
}

function sectionFragmentItems(fragment: PageFragment) {
  return fragment.section.items.slice(fragment.itemStart, fragment.itemEnd)
}

watch(sectionSpecs, schedulePagination, { deep: true, immediate: true })
watch(isExportMode, schedulePagination)

onMounted(() => {
  schedulePagination()

  resizeObserver = new ResizeObserver(() => {
    schedulePagination()
  })

  if (hostRef.value) {
    resizeObserver.observe(hostRef.value)
  }
})

onBeforeUnmount(() => {
  if (paginationFrameId != null) {
    window.cancelAnimationFrame(paginationFrameId)
  }

  resizeObserver?.disconnect()
})

defineExpose({
  getRootElement: () => rootRef.value,
})
</script>

<template>
  <div
    ref="hostRef"
    :class="['resume-document', { 'is-export': isExportMode }]"
    :data-render-mode="mode"
  >
    <div class="resume-document__measure" aria-hidden="true">
      <article class="resume-paper resume-paper--template">
        <div ref="templateInnerRef" class="resume-paper__inner" />
      </article>

      <article class="resume-paper resume-paper--measure">
        <div ref="measureRef" class="resume-paper__inner">
          <header class="paper-header" data-role="resume-header">
            <div class="paper-header__main">
              <h1>{{ previewName }}</h1>
              <p>{{ previewIntentMeta }}</p>
              <p>{{ previewBasicMeta }}</p>
              <p>{{ previewContactMeta }}</p>
            </div>
          </header>

          <template v-for="section in sectionSpecs" :key="`measure-${section.code}`">
            <section class="paper-section" :data-section-code="section.code">
              <div class="paper-section__title">{{ section.label }}</div>

              <div v-if="section.kind === 'entries'">
                <template v-for="item in section.items" :key="item.key">
                  <article
                    v-if="item.title"
                    class="paper-entry"
                    data-role="section-item"
                  >
                    <div class="paper-entry__head">
                      <strong>{{ item.title }}</strong>
                      <span>{{ item.period }}</span>
                    </div>
                    <p>{{ item.description }}</p>
                  </article>

                  <p v-else class="paper-empty" data-role="section-item">
                    {{ item.text }}
                  </p>
                </template>
              </div>

              <div v-else-if="section.kind === 'custom'">
                <template v-if="section.items[0]?.text">
                  <p class="paper-empty" data-role="section-item">
                    {{ section.items[0].text }}
                  </p>
                </template>
                <div v-else class="paper-custom-list">
                  <div
                    v-for="item in section.items"
                    :key="item.key"
                    class="paper-custom-row"
                    data-role="section-item"
                  >
                    <strong>{{ item.label }}</strong>
                    <span>{{ item.value }}</span>
                  </div>
                </div>
              </div>

              <div v-else-if="section.kind === 'pills'" class="paper-skill-list">
                <template v-for="item in section.items" :key="item.key">
                  <span
                    v-if="item.label"
                    class="paper-skill-pill"
                    data-role="section-item"
                  >
                    {{ item.label }}
                  </span>
                  <span v-else class="paper-empty" data-role="section-item">
                    {{ item.text }}
                  </span>
                </template>
              </div>

              <p v-else data-role="section-item">
                {{ section.items[0]?.text }}
              </p>
            </section>
          </template>
        </div>
      </article>
    </div>

    <div
      ref="rootRef"
      :class="['resume-pages', { 'is-export': isExportMode }]"
      :data-pagination-ready="paginationReady ? 'true' : 'false'"
      :data-page-count="pages.length"
    >
      <article
        v-for="page in pages"
        :key="page.key"
        class="resume-paper"
      >
        <div class="resume-paper__inner">
          <header v-if="page.showHeader" class="paper-header">
            <div class="paper-header__main">
              <h1>{{ previewName }}</h1>
              <p>{{ previewIntentMeta }}</p>
              <p>{{ previewBasicMeta }}</p>
              <p>{{ previewContactMeta }}</p>
            </div>
          </header>

          <template v-for="fragment in page.fragments" :key="fragment.key">
            <section class="paper-section">
              <div class="paper-section__title">{{ fragment.section.label }}</div>

              <div v-if="fragment.section.kind === 'entries'">
                <template v-for="item in sectionFragmentItems(fragment)" :key="item.key">
                  <article v-if="item.title" class="paper-entry">
                    <div class="paper-entry__head">
                      <strong>{{ item.title }}</strong>
                      <span>{{ item.period }}</span>
                    </div>
                    <p>{{ item.description }}</p>
                  </article>

                  <p v-else class="paper-empty">
                    {{ item.text }}
                  </p>
                </template>
              </div>

              <div v-else-if="fragment.section.kind === 'custom'">
                <template v-if="sectionFragmentItems(fragment)[0]?.text">
                  <p class="paper-empty">
                    {{ sectionFragmentItems(fragment)[0]?.text }}
                  </p>
                </template>
                <div v-else class="paper-custom-list">
                  <div
                    v-for="item in sectionFragmentItems(fragment)"
                    :key="item.key"
                    class="paper-custom-row"
                  >
                    <strong>{{ item.label }}</strong>
                    <span>{{ item.value }}</span>
                  </div>
                </div>
              </div>

              <div v-else-if="fragment.section.kind === 'pills'" class="paper-skill-list">
                <template v-for="item in sectionFragmentItems(fragment)" :key="item.key">
                  <span v-if="item.label" class="paper-skill-pill">
                    {{ item.label }}
                  </span>
                  <span v-else class="paper-empty">
                    {{ item.text }}
                  </span>
                </template>
              </div>

              <p v-else>
                {{ sectionFragmentItems(fragment)[0]?.text }}
              </p>
            </section>
          </template>
        </div>
      </article>
    </div>
  </div>
</template>

<style scoped>
.resume-document {
  position: relative;
  width: 100%;
}

.resume-document__measure {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 0;
  overflow: hidden;
  visibility: hidden;
  pointer-events: none;
}

.resume-pages {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 820px));
  justify-content: center;
  align-items: flex-start;
  gap: 24px;
  width: 100%;
  max-width: calc(820px * 2 + 24px);
  margin: 0 auto;
  padding: 4px 2px 12px;
}

.resume-pages[data-page-count='1'] {
  grid-template-columns: minmax(0, 820px);
}

.resume-document.is-export .resume-pages {
  display: flex;
  flex-direction: column;
  gap: 0;
  width: 794px;
  max-width: 100%;
  margin: 0 auto;
  overflow: visible;
  padding: 0;
}

.resume-paper {
  width: 100%;
  max-width: 820px;
  aspect-ratio: 210 / 297;
  border-radius: 18px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  box-shadow: 0 20px 42px rgba(15, 23, 42, 0.08);
  overflow: hidden;
  justify-self: center;
}

.resume-document.is-export .resume-paper {
  width: 794px;
  max-width: 100%;
  border: none;
  border-radius: 0;
  box-shadow: none;
}

.resume-paper--measure {
  aspect-ratio: auto;
  overflow: visible;
}

.resume-paper__inner {
  height: 100%;
  padding: 40px 52px 48px;
}

.resume-paper--measure .resume-paper__inner {
  height: auto;
  min-height: auto;
}

.paper-header {
  display: flex;
  justify-content: center;
  gap: 24px;
  align-items: flex-start;
  margin-bottom: 36px;
}

.paper-header__main {
  flex: 0 1 760px;
  text-align: center;
}

.paper-header__main h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: #111827;
}

.paper-header__main p {
  margin: 12px 0 0;
  color: #374151;
  font-size: 14px;
  line-height: 1.7;
}

.paper-section {
  margin-top: 34px;
}

.paper-section__title {
  padding-bottom: 8px;
  border-bottom: 1px solid #2f2f2f;
  font-size: 17px;
  font-weight: 700;
  color: #111827;
}

.paper-entry {
  margin-top: 16px;
}

.paper-entry__head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  font-size: 15px;
  color: #111827;
}

.paper-entry p,
.paper-section p {
  margin: 8px 0 0;
  color: #374151;
  line-height: 1.85;
  white-space: pre-line;
}

.paper-empty {
  color: #94a3b8;
  font-size: 14px;
}

.paper-skill-list {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.paper-skill-pill {
  padding: 6px 14px;
  border-radius: 999px;
  background: #eef6ff;
  color: #1e3a8a;
  font-size: 13px;
  font-weight: 600;
}

.paper-custom-list {
  margin-top: 14px;
  display: grid;
  gap: 8px;
}

.paper-custom-row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  padding-bottom: 8px;
  border-bottom: 1px dashed #e5e7eb;
}

@media (max-width: 980px) {
  .resume-pages {
    max-width: calc(700px * 2 + 24px);
    grid-template-columns: repeat(2, minmax(0, 700px));
  }

  .resume-pages[data-page-count='1'] {
    grid-template-columns: minmax(0, 700px);
  }

  .resume-paper {
    max-width: 700px;
  }

  .resume-paper__inner {
    padding: 28px 28px 36px;
  }
}

@media (max-width: 760px) {
  .resume-pages {
    grid-template-columns: minmax(0, 1fr);
    max-width: 100%;
    gap: 16px;
  }

  .resume-pages[data-page-count='1'] {
    grid-template-columns: minmax(0, 1fr);
  }

  .paper-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .paper-header__main {
    text-align: left;
  }

  .paper-entry__head,
  .paper-custom-row {
    flex-direction: column;
  }
}
</style>
