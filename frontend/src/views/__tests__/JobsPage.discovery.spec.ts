import { defineComponent, nextTick } from 'vue'
import { shallowMount, type VueWrapper } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const testState = vi.hoisted(() => ({
  route: {
    path: '/jobs',
    query: {} as Record<string, unknown>,
  },
  authStore: {
    role: 'JOBSEEKER',
  },
  client: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
  router: {
    push: vi.fn(),
    replace: vi.fn(),
  },
  message: {
    success: vi.fn(),
    error: vi.fn(),
  },
  messageBox: {
    confirm: vi.fn(),
    alert: vi.fn(),
  },
}))

vi.mock('vue-router', () => ({
  useRoute: () => testState.route,
  useRouter: () => testState.router,
}))

vi.mock('../../stores/auth', () => ({
  useAuthStore: () => testState.authStore,
}))

vi.mock('../../api/client', () => ({
  default: testState.client,
}))

vi.mock('element-plus', () => ({
  ElMessage: testState.message,
  ElMessageBox: testState.messageBox,
}))

import JobsPage from '../JobsPage.vue'

type PartialJob = Partial<{
  id: number
  jobCode: string
  title: string
  category: string
  location: string
  salaryMin: number
  salaryMax: number
  experienceRequirement: string
  educationRequirement: string
  headcount: number
  description: string
  skillTags: string[]
  companyUserId: number
  companyName: string
  benefitTags: string[]
}>

const slotStub = defineComponent({
  template: '<div><slot /></div>',
})

const buttonStub = defineComponent({
  emits: ['click'],
  template: '<button type="button" @click="$emit(\'click\', $event)"><slot /></button>',
})

const tagStub = defineComponent({
  template: '<span class="el-tag-stub"><slot /></span>',
})

const tooltipStub = defineComponent({
  template: '<div><slot /></div>',
})

const dialogStub = defineComponent({
  props: {
    modelValue: {
      type: Boolean,
      default: false,
    },
  },
  template: '<div v-if="modelValue"><slot /><slot name="footer" /></div>',
})

const emptyStub = defineComponent({
  props: {
    description: {
      type: String,
      default: '',
    },
  },
  template: `
    <div class="el-empty-stub">
      <slot name="image" />
      <slot name="description">
        <p>{{ description }}</p>
      </slot>
      <slot />
    </div>
  `,
})

function createJob(overrides: PartialJob = {}) {
  return {
    id: overrides.id ?? 1,
    jobCode: overrides.jobCode ?? `JOB-${overrides.id ?? 1}`,
    title: overrides.title ?? '后端工程师',
    category: overrides.category ?? '后端开发',
    location: overrides.location ?? '上海',
    salaryMin: overrides.salaryMin ?? 15000,
    salaryMax: overrides.salaryMax ?? 25000,
    experienceRequirement: overrides.experienceRequirement ?? '3年及以上',
    educationRequirement: overrides.educationRequirement ?? '本科及以上',
    headcount: overrides.headcount ?? 2,
    description: overrides.description ?? '负责岗位大厅列表、筛选与排序体验优化。',
    skillTags: overrides.skillTags ?? ['Vue3', 'TypeScript'],
    companyUserId: overrides.companyUserId ?? 1001,
    companyName: overrides.companyName ?? '轻舟招聘科技',
    benefitTags: overrides.benefitTags ?? ['五险一金', '弹性工作'],
    recentlyApplied: false,
    favorited: false,
  }
}

function createSearchPage(records: ReturnType<typeof createJob>[]) {
  return {
    pageNum: 1,
    pageSize: 1000,
    total: records.length,
    records,
  }
}

function configureClientGet(options?: {
  searchJobs?: ReturnType<typeof createJob>[]
  detailJob?: ReturnType<typeof createJob>
}) {
  const searchJobs = options?.searchJobs ?? [createJob()]
  const detailJob = options?.detailJob ?? searchJobs[0] ?? createJob()

  testState.client.get.mockImplementation(async (url: string) => {
    if (url === '/jobseeker/applications') {
      return {
        data: {
          pageNum: 1,
          pageSize: 200,
          total: 0,
          records: [],
        },
      }
    }

    if (url === '/jobs/search') {
      return { data: createSearchPage(searchJobs) }
    }

    if (url.startsWith('/jobs/')) {
      return { data: detailJob }
    }

    throw new Error(`Unexpected GET request: ${url}`)
  })
}

function mountJobsPage() {
  return shallowMount(JobsPage, {
    global: {
      directives: {
        loading: () => undefined,
      },
      stubs: {
        'el-button': buttonStub,
        'el-cascader': slotStub,
        'el-date-picker': slotStub,
        'el-dialog': dialogStub,
        'el-empty': emptyStub,
        'el-form': slotStub,
        'el-form-item': slotStub,
        'el-input': slotStub,
        'el-input-number': slotStub,
        'el-option': slotStub,
        'el-pagination': slotStub,
        'el-select': slotStub,
        'el-table': slotStub,
        'el-table-column': slotStub,
        'el-tag': tagStub,
        'el-tooltip': tooltipStub,
      },
    },
  })
}

function setupState(wrapper: VueWrapper) {
  return (wrapper.vm as unknown as { $?: { setupState?: Record<string, unknown> } }).$?.setupState
    ?? (wrapper.vm as unknown as Record<string, unknown>)
}

async function flushComponentWork() {
  await Promise.resolve()
  await nextTick()
  await Promise.resolve()
  await nextTick()
}

async function triggerDebouncedRefresh(wrapper: VueWrapper) {
  await nextTick()
  vi.advanceTimersByTime(320)
  await flushComponentWork()
  return setupState(wrapper)
}

describe('JobsPage job list flow', () => {
  beforeEach(() => {
    testState.route.path = '/jobs'
    testState.route.query = {}
    testState.authStore.role = 'JOBSEEKER'
    testState.client.get.mockReset()
    testState.client.post.mockReset()
    testState.client.put.mockReset()
    testState.client.patch.mockReset()
    testState.client.delete.mockReset()
    testState.router.push.mockReset()
    testState.router.replace.mockReset()
    testState.message.success.mockReset()
    testState.message.error.mockReset()
    testState.messageBox.confirm.mockReset()
    testState.messageBox.alert.mockReset()
    vi.useRealTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('loads the default job list through /jobs/search on mount', async () => {
    configureClientGet({
      searchJobs: [createJob({ title: '默认岗位列表' })],
    })

    const wrapper = mountJobsPage()
    await flushComponentWork()

    expect(wrapper.text()).toContain('岗位列表')
    expect(wrapper.text()).toContain('默认岗位列表')
    expect(testState.client.get).toHaveBeenCalledWith('/jobs/search', expect.objectContaining({
      params: expect.objectContaining({
        sortKey: 'default',
      }),
    }))
    expect(testState.client.get).not.toHaveBeenCalledWith('/jobs/recommend', expect.anything())
  })

  it('uses the same search endpoint when only filters are present', async () => {
    vi.useFakeTimers()
    configureClientGet({
      searchJobs: [createJob({ id: 2, title: '福利筛选岗位', benefitTags: ['五险一金'] })],
    })

    const wrapper = mountJobsPage()
    await flushComponentWork()

    const state = setupState(wrapper) as {
      searchForm: {
        category: string
        benefitTags: string[]
      }
    }
    state.searchForm.category = '后端开发'
    state.searchForm.benefitTags = ['五险一金']
    await triggerDebouncedRefresh(wrapper)

    expect(wrapper.text()).toContain('条件筛选')
    expect(wrapper.text()).toContain('福利筛选岗位')
    expect(testState.client.get).toHaveBeenLastCalledWith('/jobs/search', expect.objectContaining({
      params: expect.objectContaining({
        category: '后端开发',
        benefitTags: ['五险一金'],
        sortKey: 'default',
      }),
    }))
  })

  it('uses keyword search with the same endpoint and selected sort key', async () => {
    vi.useFakeTimers()
    configureClientGet({
      searchJobs: [createJob({ id: 3, title: '关键词岗位', companyName: '搜索科技' })],
    })

    const wrapper = mountJobsPage()
    await flushComponentWork()

    const state = setupState(wrapper) as {
      searchForm: {
        keyword: string
        sortKey: 'default' | 'latest' | 'salary'
      }
    }
    state.searchForm.keyword = '后端'
    state.searchForm.sortKey = 'salary'
    await triggerDebouncedRefresh(wrapper)

    expect(wrapper.text()).toContain('关键词搜索')
    expect(wrapper.text()).toContain('关键词岗位')
    expect(testState.client.get).toHaveBeenLastCalledWith('/jobs/search', expect.objectContaining({
      params: expect.objectContaining({
        keyword: '后端',
        sortKey: 'salary',
      }),
    }))
  })

  it('restores the default list after clearing filters', async () => {
    vi.useFakeTimers()
    configureClientGet({
      searchJobs: [createJob({ title: '回到默认列表岗位' })],
    })

    const wrapper = mountJobsPage()
    await flushComponentWork()

    const state = setupState(wrapper) as {
      searchForm: {
        keyword: string
        benefitTags: string[]
      }
      clearSearchFilters: () => Promise<void>
      discoveryModeLabel: string
    }
    state.searchForm.keyword = '运营'
    state.searchForm.benefitTags = ['带薪年假']
    await triggerDebouncedRefresh(wrapper)
    await state.clearSearchFilters()
    await flushComponentWork()

    expect(state.searchForm.keyword).toBe('')
    expect(state.searchForm.benefitTags).toEqual([])
    expect(state.discoveryModeLabel).toBe('默认列表')
    expect(testState.client.get).toHaveBeenLastCalledWith('/jobs/search', expect.objectContaining({
      params: expect.objectContaining({
        keyword: '',
        benefitTags: [],
        sortKey: 'default',
      }),
    }))
  })

  it('shows the empty state copy when filtered results are empty', async () => {
    vi.useFakeTimers()
    configureClientGet({
      searchJobs: [],
    })

    const wrapper = mountJobsPage()
    await flushComponentWork()

    const state = setupState(wrapper) as {
      searchForm: {
        keyword: string
      }
    }
    state.searchForm.keyword = '没有结果'
    await triggerDebouncedRefresh(wrapper)

    expect(wrapper.text()).toContain('暂无符合条件的岗位')
    expect(wrapper.text()).toContain('可以尝试更换关键词、放宽筛选范围或清空筛选重新查看岗位。')
    expect(wrapper.text()).toContain('清空筛选')
  })
})
