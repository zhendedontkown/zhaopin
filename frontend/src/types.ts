export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageResponse<T> {
  pageNum: number
  pageSize: number
  total: number
  records: T[]
}

export type RoleCode = 'ADMIN' | 'COMPANY' | 'JOBSEEKER' | ''
export type UserAccountStatus = 'ACTIVE' | 'DISABLED' | 'DELETED'

export interface UserSession {
  userId: number
  token: string
  role: RoleCode
  displayName: string
}

export interface CompanyProfile {
  id?: number
  userId: number
  companyName: string
  unifiedSocialCreditCode: string
  contactPerson: string
  phone: string
  email: string
  address?: string
  description?: string
  auditStatus: string
}

export interface JobseekerProfile {
  id?: number
  userId: number
  fullName: string
  phone: string
  email: string
}

export interface UserProfileResponse {
  userId: number
  displayName: string
  email: string
  phone: string
  roles: RoleCode[]
  primaryRole: RoleCode
  workspaceLabel: string
  companyProfile?: CompanyProfile
  jobseekerProfile?: JobseekerProfile
}

export interface PasswordChangePayload {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

export interface JobRecord {
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
  skillTags?: string[]
  benefitTags?: string[]
  status: string
  publishedAt?: string
  expireAt?: string
  companyUserId: number
  companyName: string
  recentlyApplied?: boolean
  favorited?: boolean
  createdAt?: string
}

export interface AdminDashboardStats {
  userCount: number
  companyUserCount: number
  jobseekerUserCount: number
  pendingCompanyAuditCount: number
  jobCount: number
  applicationCount: number
  interviewingCount: number
  offeredCount: number
  rejectedCount: number
}

export interface AdminCompanyUserRecord {
  userId: number
  displayName: string
  companyName: string
  contactPerson: string
  phone: string
  email: string
  unifiedSocialCreditCode: string
  auditStatus: string
  userStatus: UserAccountStatus
  createdAt: string
}

export interface AdminCompanyAuditRecord extends AdminCompanyUserRecord {}

export interface AdminJobseekerUserRecord {
  userId: number
  displayName: string
  fullName: string
  phone: string
  email: string
  highestEducation?: string
  desiredPositionCategory?: string
  userStatus: UserAccountStatus
  createdAt: string
}

export interface AdminManagedJobRecord {
  id: number
  jobCode: string
  title: string
  category: string
  location: string
  salaryMin: number
  salaryMax: number
  experienceRequirement: string
  educationRequirement: string
  status: string
  publishedAt?: string
  createdAt?: string
  companyUserId: number
  companyName: string
}

export interface AdminApplicationRecord {
  id: number
  jobId: number
  jobTitle: string
  companyUserId: number
  companyName: string
  jobseekerUserId: number
  jobseekerName: string
  resumeId: number
  status: string
  statusText: string
  statusDescription: string
  statusRemark?: string
  appliedAt: string
  statusUpdatedAt?: string
}

export interface ResumeEducation {
  id?: number
  schoolName: string
  major?: string
  degree?: string
  startDate?: string
  endDate?: string
  current?: boolean
  description?: string
  sortOrder?: number
}

export interface ResumeExperience {
  id?: number
  companyName: string
  jobTitle: string
  startDate?: string
  endDate?: string
  current?: boolean
  description?: string
  sortOrder?: number
}

export interface ResumeProject {
  id?: number
  projectName: string
  roleName?: string
  startDate?: string
  endDate?: string
  current?: boolean
  description?: string
  sortOrder?: number
}

export interface ResumeModuleConfigItem {
  code: string
  label: string
  visible: boolean
  order: number
}

export interface ResumeExtraSectionItem {
  id?: number
  title: string
  subtitle?: string
  startDate?: string
  endDate?: string
  current?: boolean
  description?: string
  sortOrder?: number
}

export interface ResumeCustomFieldItem {
  key: string
  value: string
  sortOrder?: number
}

export interface ResumeCore {
  id?: number
  templateCode: string
  fullName?: string
  gender?: string
  age?: number
  birthDate?: string
  displayAge?: boolean
  phone?: string
  email?: string
  city?: string
  summary?: string
  expectedCategory?: string
  expectedSalaryMin?: number
  expectedSalaryMax?: number
  highestEducation?: string
  yearsOfExperience?: number
  completenessScore?: number
}

export interface ResumePayload extends ResumeCore {
  moduleConfig: ResumeModuleConfigItem[]
  educations: ResumeEducation[]
  experiences: ResumeExperience[]
  projects: ResumeProject[]
  internships: ResumeExtraSectionItem[]
  campusExperiences: ResumeExtraSectionItem[]
  honors: ResumeExtraSectionItem[]
  hobbies: ResumeExtraSectionItem[]
  customFields: ResumeCustomFieldItem[]
  skills: string[]
}

export interface ResumeDetail {
  resume: ResumeCore
  moduleConfig: ResumeModuleConfigItem[]
  educations: ResumeEducation[]
  experiences: ResumeExperience[]
  projects: ResumeProject[]
  internships: ResumeExtraSectionItem[]
  campusExperiences: ResumeExtraSectionItem[]
  honors: ResumeExtraSectionItem[]
  hobbies: ResumeExtraSectionItem[]
  customFields: ResumeCustomFieldItem[]
  skills: string[]
  missingItems: string[]
  completenessScore: number
}

export interface SavedResumeSummary {
  id: number
  name: string
  templateCode: string
  completenessScore: number
  completeFlag: boolean
  createdAt: string
  updatedAt: string
}

export interface SavedResumeDetail {
  id: number
  name: string
  templateCode: string
  completenessScore: number
  completeFlag: boolean
  missingItems: string[]
  resumeDetail: ResumeDetail
  createdAt: string
  updatedAt: string
}

export interface CreateSavedResumeRequest {
  name: string
  draft: ResumePayload
}

export interface CreateSavedResumeResponse {
  savedResume: SavedResumeSummary
  currentDraft: ResumeDetail
}

export interface UpdateSavedResumeRequest {
  name: string
  draft: ResumePayload
}

export interface UpdateSavedResumeResponse {
  savedResume: SavedResumeSummary
  currentDraft: ResumeDetail
}

export interface ApplicationResumeViewResponse {
  applicationId: number
  resumeId: number
  savedResumeId?: number
  savedResumeName?: string
  jobTitle: string
  companyName: string
  snapshotBased: boolean
  resumeSource: 'SNAPSHOT' | 'CURRENT'
  resumeDetail: ResumeDetail
}

export interface ApplyJobRequest {
  jobId: number
  savedResumeId: number
}

export type ApplicationStatusCode =
  | 'SUBMITTED'
  | 'VIEWED'
  | 'INTERVIEW_PENDING'
  | 'REJECTED'
  | 'INTERVIEWING'
  | 'OFFERED'

export interface ApplicationRecord {
  id: number
  jobId: number
  jobTitle: string
  companyUserId: number
  companyName: string
  jobseekerUserId: number
  resumeId: number
  savedResumeId?: number
  savedResumeName?: string
  status: ApplicationStatusCode
  statusText: string
  statusDescription: string
  statusRemark?: string
  appliedAt: string
  viewedAt?: string
  statusUpdatedAt?: string
}

export interface NotificationRecord {
  id: number
  userId: number
  type: string
  title: string
  content: string
  readFlag: number
  relatedUserId?: number
  relatedConversationId?: number
  relatedApplicationId?: number
  createdAt: string
}

export interface ConversationRecord {
  id: number
  companyUserId: number
  jobseekerUserId: number
  companyName?: string
  jobseekerName?: string
  peerUserId: number
  peerName: string
  lastMessage: string
  lastMessageAt?: string
}

export interface ChatMessageRecord {
  id: number
  conversationId: number
  senderUserId: number
  receiverUserId: number
  content: string
  readFlag: number
  createdAt: string
}
