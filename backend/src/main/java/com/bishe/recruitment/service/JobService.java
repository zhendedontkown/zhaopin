package com.bishe.recruitment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bishe.recruitment.common.BusinessException;
import com.bishe.recruitment.common.PageResponse;
import com.bishe.recruitment.dto.AdminDtos;
import com.bishe.recruitment.dto.JobDtos;
import com.bishe.recruitment.entity.AdminActionLog;
import com.bishe.recruitment.entity.CompanyProfile;
import com.bishe.recruitment.entity.JobApplication;
import com.bishe.recruitment.entity.JobFavorite;
import com.bishe.recruitment.entity.JobPost;
import com.bishe.recruitment.enums.CompanyAuditStatus;
import com.bishe.recruitment.enums.JobStatus;
import com.bishe.recruitment.enums.NotificationType;
import com.bishe.recruitment.mapper.AdminActionLogMapper;
import com.bishe.recruitment.mapper.CompanyProfileMapper;
import com.bishe.recruitment.mapper.JobApplicationMapper;
import com.bishe.recruitment.mapper.JobFavoriteMapper;
import com.bishe.recruitment.mapper.JobPostMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class JobService {

    private static final int MAX_TAGS = 12;
    private static final int MAX_TAG_LENGTH = 16;

    private final JobPostMapper jobPostMapper;
    private final CompanyProfileMapper companyProfileMapper;
    private final JobApplicationMapper jobApplicationMapper;
    private final JobFavoriteMapper jobFavoriteMapper;
    private final NotificationService notificationService;
    private final AdminActionLogMapper adminActionLogMapper;
    private final ObjectMapper objectMapper;

    public JobService(JobPostMapper jobPostMapper, CompanyProfileMapper companyProfileMapper,
                      JobApplicationMapper jobApplicationMapper, JobFavoriteMapper jobFavoriteMapper,
                      NotificationService notificationService, AdminActionLogMapper adminActionLogMapper,
                      ObjectMapper objectMapper) {
        this.jobPostMapper = jobPostMapper;
        this.companyProfileMapper = companyProfileMapper;
        this.jobApplicationMapper = jobApplicationMapper;
        this.jobFavoriteMapper = jobFavoriteMapper;
        this.notificationService = notificationService;
        this.adminActionLogMapper = adminActionLogMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> createJob(Long companyUserId, JobDtos.JobSaveRequest request) {
        validateSalary(request.getSalaryMin(), request.getSalaryMax());
        JobPost jobPost = new JobPost();
        fillJobFields(jobPost, request);
        jobPost.setCompanyUserId(companyUserId);
        jobPost.setJobCode(generateJobCode());
        jobPost.setStatus(JobStatus.DRAFT.name());
        jobPost.setDeletedFlag(0);
        jobPostMapper.insert(jobPost);
        return buildBaseJobView(jobPost, null, Set.of(), Set.of());
    }

    @Transactional
    public Map<String, Object> updateJob(Long companyUserId, Long jobId, JobDtos.JobSaveRequest request) {
        validateSalary(request.getSalaryMin(), request.getSalaryMax());
        JobPost jobPost = getOwnedJob(companyUserId, jobId);
        fillJobFields(jobPost, request);
        if (JobStatus.PUBLISHED.name().equals(jobPost.getStatus()) && request.getDescription().length() < 200) {
            throw new BusinessException("已发布岗位的岗位描述不能少于200字");
        }
        jobPostMapper.updateById(jobPost);
        return buildBaseJobView(jobPost, null, Set.of(), Set.of());
    }

    public void deleteJob(Long companyUserId, Long jobId) {
        JobPost jobPost = getOwnedJob(companyUserId, jobId);
        jobPost.setDeletedFlag(1);
        jobPost.setDeletedAt(LocalDateTime.now());
        jobPost.setDeletedBy(companyUserId);
        jobPost.setStatus(JobStatus.OFFLINE.name());
        jobPostMapper.updateById(jobPost);
    }

    public PageResponse<Map<String, Object>> listCompanyJobs(Long companyUserId, String status, long pageNum, long pageSize) {
        Page<JobPost> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<JobPost> wrapper = new LambdaQueryWrapper<JobPost>()
                .eq(JobPost::getCompanyUserId, companyUserId)
                .and(condition -> condition.isNull(JobPost::getDeletedFlag).or().eq(JobPost::getDeletedFlag, 0))
                .orderByDesc(JobPost::getCreatedAt);
        if (StringUtils.hasText(status)) {
            wrapper.eq(JobPost::getStatus, status);
        }
        Page<JobPost> result = jobPostMapper.selectPage(page, wrapper);
        List<Map<String, Object>> records = result.getRecords().stream()
                .map(job -> buildBaseJobView(job, null, Set.of(), Set.of()))
                .toList();
        return PageResponse.<Map<String, Object>>builder()
                .pageNum(result.getCurrent())
                .pageSize(result.getSize())
                .total(result.getTotal())
                .records(records)
                .build();
    }

    @Transactional
    public Map<String, Object> updateJobStatusByCompany(Long companyUserId, Long jobId, String status) {
        JobStatus targetStatus = parseStatus(status);
        JobPost jobPost = getOwnedJob(companyUserId, jobId);
        if (targetStatus == JobStatus.PUBLISHED) {
            ensureCompanyApproved(companyUserId);
            if (jobPost.getDescription() == null || jobPost.getDescription().length() < 200) {
                throw new BusinessException("岗位描述不少于200字才可发布");
            }
            if (jobPost.getExpireAt() != null && jobPost.getExpireAt().isBefore(LocalDateTime.now())) {
                throw new BusinessException("岗位已过期，请重新设置过期时间");
            }
            jobPost.setPublishedAt(LocalDateTime.now());
        }
        if (targetStatus == JobStatus.EXPIRED
                && (jobPost.getExpireAt() == null || jobPost.getExpireAt().isAfter(LocalDateTime.now()))) {
            throw new BusinessException("未到过期时间，不能手动设置为已过期");
        }
        jobPost.setStatus(targetStatus.name());
        jobPostMapper.updateById(jobPost);
        return buildBaseJobView(jobPost, null, Set.of(), Set.of());
    }

    @Transactional
    public Map<String, Object> updateJobStatusByAdmin(Long jobId, String status) {
        JobStatus targetStatus = parseStatus(status);
        JobPost jobPost = getJobById(jobId);
        jobPost.setStatus(targetStatus.name());
        if (targetStatus == JobStatus.PUBLISHED) {
            jobPost.setPublishedAt(LocalDateTime.now());
        }
        jobPostMapper.updateById(jobPost);
        notificationService.createAndPush(jobPost.getCompanyUserId(), NotificationType.JOB_STATUS,
                "岗位状态已更新", "岗位 " + jobPost.getTitle() + " 状态已调整为 " + targetStatus.name());
        return buildBaseJobView(jobPost, null, Set.of(), Set.of());
    }

    public PageResponse<AdminDtos.ManagedJobView> listJobsForAdmin(String keyword, String companyKeyword,
                                                                   String status, String category,
                                                                   long pageNum, long pageSize) {
        List<JobPost> jobs = jobPostMapper.selectList(new LambdaQueryWrapper<JobPost>()
                .and(condition -> condition.isNull(JobPost::getDeletedFlag).or().eq(JobPost::getDeletedFlag, 0))
                .orderByDesc(JobPost::getCreatedAt));
        Map<Long, String> companyNames = getCompanyNamesByUserIds(jobs.stream()
                .map(JobPost::getCompanyUserId)
                .collect(Collectors.toSet()));

        List<AdminDtos.ManagedJobView> records = jobs.stream()
                .map(job -> buildManagedJobView(job, companyNames.get(job.getCompanyUserId())))
                .filter(view -> matchesAdminJobKeyword(keyword, view))
                .filter(view -> matchesAdminCompanyKeyword(companyKeyword, view.getCompanyName()))
                .filter(view -> matchesAdminJobStatus(status, view.getStatus()))
                .filter(view -> matchesAdminJobCategory(category, view.getCategory()))
                .toList();
        return paginateItems(records, pageNum, pageSize);
    }

    @Transactional
    public AdminDtos.ManagedJobView moderateJobByAdmin(Long adminUserId, Long jobId, String action, String reason) {
        String normalizedAction = normalizeModerationAction(action);
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException("请填写处理原因");
        }

        JobPost jobPost = getJobById(jobId);
        String companyName = resolveCompanyName(jobPost.getCompanyUserId());
        if ("OFFLINE".equals(normalizedAction)) {
            if (!JobStatus.OFFLINE.name().equals(jobPost.getStatus())) {
                jobPost.setStatus(JobStatus.OFFLINE.name());
                jobPostMapper.updateById(jobPost);
            }
            notificationService.createAndPush(
                    jobPost.getCompanyUserId(),
                    NotificationType.JOB_STATUS,
                    "岗位已被平台下线",
                    "岗位 " + jobPost.getTitle() + " 已被平台下线，原因：" + reason.trim() + "。");
        } else {
            jobPost.setDeletedFlag(1);
            jobPost.setDeletedAt(LocalDateTime.now());
            jobPost.setDeletedBy(adminUserId);
            jobPost.setStatus(JobStatus.OFFLINE.name());
            jobPostMapper.updateById(jobPost);
            notificationService.createAndPush(
                    jobPost.getCompanyUserId(),
                    NotificationType.JOB_STATUS,
                    "岗位已被平台删除",
                    "岗位 " + jobPost.getTitle() + " 已被平台删除，原因：" + reason.trim() + "。");
        }

        recordAdminAction(
                "JOB",
                jobPost.getId(),
                normalizedAction,
                reason.trim(),
                adminUserId,
                Map.of(
                        "jobTitle", jobPost.getTitle(),
                        "companyName", companyName,
                        "status", jobPost.getStatus()));

        return buildManagedJobView(jobPost, companyName);
    }

    public PageResponse<Map<String, Object>> searchJobs(JobDtos.JobSearchRequest request, Long currentUserId) {
        refreshExpiredJobs();

        JobSearchCriteria criteria = buildSearchCriteria(request);
        Set<Long> recentAppliedJobIds = getRecentAppliedJobIds(currentUserId);
        Set<Long> favoriteJobIds = getFavoriteJobIds(currentUserId);
        List<JobPost> jobs = listActivePublishedJobs();
        Map<Long, String> companyNames = getCompanyNamesByUserIds(jobs.stream()
                .map(JobPost::getCompanyUserId)
                .collect(Collectors.toSet()));

        List<Map<String, Object>> views = jobs.stream()
                .filter(job -> matchesKeyword(job, companyNames.get(job.getCompanyUserId()), criteria.keyword()))
                .filter(job -> filterJobsByCriteria(job, criteria))
                .map(job -> buildBaseJobView(
                        job,
                        currentUserId,
                        recentAppliedJobIds,
                        favoriteJobIds,
                        companyNames.get(job.getCompanyUserId())))
                .toList();

        views = sortViews(views, criteria.sortKey());
        return paginate(views, request == null ? null : request.getPageNum(), request == null ? null : request.getPageSize());
    }

    public Map<String, Object> getJobDetail(Long jobId, Long currentUserId) {
        refreshExpiredJobs();
        JobPost jobPost = getJobById(jobId);
        if (!JobStatus.PUBLISHED.name().equals(jobPost.getStatus()) && currentUserId == null) {
            throw new BusinessException("岗位不存在或不可查看");
        }
        return buildBaseJobView(jobPost, currentUserId, getRecentAppliedJobIds(currentUserId), getFavoriteJobIds(currentUserId));
    }

    public JobPost getJobById(Long jobId) {
        JobPost jobPost = jobPostMapper.selectById(jobId);
        if (jobPost != null && Integer.valueOf(1).equals(jobPost.getDeletedFlag())) {
            jobPost = null;
        }
        if (jobPost == null) {
            throw new BusinessException("岗位不存在");
        }
        return jobPost;
    }

    public Map<String, Object> buildJobViewForUser(JobPost job, Long currentUserId) {
        return buildBaseJobView(job, currentUserId, getRecentAppliedJobIds(currentUserId), getFavoriteJobIds(currentUserId));
    }

    public List<Map<String, Object>> buildJobViewsForUser(List<JobPost> jobs, Long currentUserId) {
        Set<Long> recentAppliedJobIds = getRecentAppliedJobIds(currentUserId);
        Set<Long> favoriteJobIds = getFavoriteJobIds(currentUserId);
        return jobs.stream()
                .map(job -> buildBaseJobView(job, currentUserId, recentAppliedJobIds, favoriteJobIds))
                .toList();
    }

    private void fillJobFields(JobPost jobPost, JobDtos.JobSaveRequest request) {
        jobPost.setTitle(request.getTitle());
        jobPost.setCategory(request.getCategory());
        jobPost.setLocation(request.getLocation());
        jobPost.setSalaryMin(request.getSalaryMin());
        jobPost.setSalaryMax(request.getSalaryMax());
        jobPost.setExperienceRequirement(request.getExperienceRequirement());
        jobPost.setEducationRequirement(request.getEducationRequirement());
        jobPost.setHeadcount(request.getHeadcount());
        jobPost.setDescription(request.getDescription());
        jobPost.setBenefitTagsJson(writeTagList(normalizeTagListForSave(request.getBenefitTags(), "福利标签")));
        jobPost.setSkillTagsJson(writeTagList(normalizeTagListForSave(request.getSkillTags(), "技能标签")));
        jobPost.setExpireAt(request.getExpireAt());
    }

    private List<String> normalizeTagListForSave(List<String> rawTags, String tagType) {
        if (rawTags == null) {
            return List.of();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String rawTag : rawTags) {
            String tag = trimToNull(rawTag);
            if (tag == null) {
                continue;
            }
            if (tag.length() > MAX_TAG_LENGTH) {
                throw new BusinessException(tagType + "长度不能超过16个字符");
            }
            normalized.add(tag);
        }
        if (normalized.size() > MAX_TAGS) {
            throw new BusinessException(tagType + "最多选择12个");
        }
        return List.copyOf(normalized);
    }

    private List<String> sanitizeTagList(List<String> rawTags) {
        if (rawTags == null) {
            return List.of();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String rawTag : rawTags) {
            String tag = trimToNull(rawTag);
            if (tag == null || tag.length() > MAX_TAG_LENGTH) {
                continue;
            }
            normalized.add(tag);
            if (normalized.size() >= MAX_TAGS) {
                break;
            }
        }
        return List.copyOf(normalized);
    }

    private String writeTagList(List<String> tags) {
        try {
            return objectMapper.writeValueAsString(tags == null ? List.of() : tags);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("标签保存失败");
        }
    }

    private List<String> readTagList(String rawJson) {
        if (!StringUtils.hasText(rawJson)) {
            return List.of();
        }

        try {
            List<String> tags = objectMapper.readValue(rawJson, new TypeReference<List<String>>() { });
            return sanitizeTagList(tags);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private void validateSalary(Integer salaryMin, Integer salaryMax) {
        if (salaryMin != null && salaryMax != null && salaryMin > salaryMax) {
            throw new BusinessException("最低薪资不能高于最高薪资");
        }
    }

    private JobPost getOwnedJob(Long companyUserId, Long jobId) {
        JobPost jobPost = getJobById(jobId);
        if (!Objects.equals(jobPost.getCompanyUserId(), companyUserId)) {
            throw new BusinessException("只能操作本企业的岗位");
        }
        return jobPost;
    }

    private void ensureCompanyApproved(Long companyUserId) {
        CompanyProfile profile = companyProfileMapper.selectOne(new LambdaQueryWrapper<CompanyProfile>()
                .eq(CompanyProfile::getUserId, companyUserId));
        if (profile == null || !CompanyAuditStatus.APPROVED.name().equals(profile.getAuditStatus())) {
            throw new BusinessException("企业认证通过后才可发布岗位");
        }
    }

    private String generateJobCode() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = LocalDateTime.of(today, LocalTime.MAX);
        long count = jobPostMapper.selectCount(new LambdaQueryWrapper<JobPost>()
                .between(JobPost::getCreatedAt, start, end));
        return "JOB" + today.format(DateTimeFormatter.BASIC_ISO_DATE) + String.format("%04d", count + 1);
    }

    private JobStatus parseStatus(String status) {
        try {
            return JobStatus.valueOf(status);
        } catch (Exception ex) {
            throw new BusinessException("不支持的岗位状态: " + status);
        }
    }

    private List<JobPost> listActivePublishedJobs() {
        return jobPostMapper.selectList(new LambdaQueryWrapper<JobPost>()
                .eq(JobPost::getStatus, JobStatus.PUBLISHED.name())
                .and(wrapper -> wrapper.isNull(JobPost::getDeletedFlag).or().eq(JobPost::getDeletedFlag, 0))
                .and(wrapper -> wrapper.isNull(JobPost::getExpireAt).or().gt(JobPost::getExpireAt, LocalDateTime.now()))
                .orderByDesc(JobPost::getPublishedAt));
    }

    private JobSearchCriteria buildSearchCriteria(JobDtos.JobSearchRequest request) {
        if (request == null) {
            return JobSearchCriteria.empty();
        }
        validateSalary(request.getSalaryMin(), request.getSalaryMax());
        String experienceRequirement = normalizeOptionalRequirement(request.getExperienceRequirement());
        String educationRequirement = normalizeOptionalRequirement(request.getEducationRequirement());
        return new JobSearchCriteria(
                trimToNull(request.getKeyword()),
                trimToNull(request.getCategory()),
                trimToNull(request.getLocation()),
                request.getSalaryMin(),
                request.getSalaryMax(),
                experienceRequirement,
                parseYears(experienceRequirement),
                educationRequirement,
                sanitizeTagList(request.getBenefitTags()),
                normalizeSortKey(request.getSortKey())
        );
    }

    private boolean filterJobsByCriteria(JobPost job, JobSearchCriteria criteria) {
        if (criteria == null) {
            return true;
        }
        if (StringUtils.hasText(criteria.category()) && !matchesCategoryFilter(job, criteria.category())) {
            return false;
        }
        if (StringUtils.hasText(criteria.location()) && !matchesLocationFilter(job, criteria.location())) {
            return false;
        }
        if ((criteria.salaryMin() != null || criteria.salaryMax() != null)
                && !matchesSalaryRange(job, criteria.salaryMin(), criteria.salaryMax())) {
            return false;
        }
        if (StringUtils.hasText(criteria.experienceRequirement())
                && !matchesExperienceFilter(job, criteria.experienceRequirement(), criteria.experienceYears())) {
            return false;
        }
        if (StringUtils.hasText(criteria.educationRequirement())
                && !matchesEducationFilter(job, criteria.educationRequirement())) {
            return false;
        }
        return criteria.benefitTags().isEmpty()
                || intersectCount(readTagList(job.getBenefitTagsJson()), criteria.benefitTags()) > 0;
    }

    private boolean matchesKeyword(JobPost job, String companyName, String keyword) {
        String normalizedKeyword = normalizeSearchText(keyword);
        if (!StringUtils.hasText(normalizedKeyword)) {
            return true;
        }
        return containsKeyword(job.getTitle(), normalizedKeyword)
                || containsKeyword(companyName, normalizedKeyword)
                || containsKeyword(job.getCategory(), normalizedKeyword)
                || containsKeyword(job.getDescription(), normalizedKeyword);
    }

    private boolean containsKeyword(String value, String normalizedKeyword) {
        return normalizeSearchText(value).contains(normalizedKeyword);
    }

    private boolean matchesCategoryFilter(JobPost job, String category) {
        String normalizedCategory = normalizeSearchText(category);
        String jobCategory = normalizeSearchText(job.getCategory());
        return jobCategory.equals(normalizedCategory)
                || jobCategory.contains(normalizedCategory)
                || normalizeSearchText(job.getTitle()).contains(normalizedCategory);
    }

    private boolean matchesLocationFilter(JobPost job, String location) {
        return normalizeSearchText(job.getLocation()).contains(normalizeSearchText(location));
    }

    private boolean matchesSalaryRange(JobPost job, Integer salaryMin, Integer salaryMax) {
        int requestedMin = salaryMin == null ? 0 : salaryMin;
        int requestedMax = salaryMax == null ? Integer.MAX_VALUE : salaryMax;
        return job.getSalaryMax() >= requestedMin && job.getSalaryMin() <= requestedMax;
    }

    private boolean matchesExperienceFilter(JobPost job, String experienceRequirement, Integer experienceYears) {
        Integer jobRequiredYears = parseYears(job.getExperienceRequirement());
        if (experienceYears != null && jobRequiredYears != null) {
            return jobRequiredYears <= experienceYears;
        }
        return normalizeSearchText(job.getExperienceRequirement()).contains(normalizeSearchText(experienceRequirement));
    }

    private boolean matchesEducationFilter(JobPost job, String educationRequirement) {
        Integer selectedRank = educationRank(educationRequirement);
        Integer requiredRank = educationRank(job.getEducationRequirement());
        if (selectedRank != null && requiredRank != null) {
            return selectedRank >= requiredRank;
        }
        return normalizeSearchText(job.getEducationRequirement()).contains(normalizeSearchText(educationRequirement));
    }

    private int intersectCount(List<String> left, List<String> right) {
        if (left == null || left.isEmpty() || right == null || right.isEmpty()) {
            return 0;
        }
        Set<String> normalizedLeft = left.stream()
                .map(this::normalizeSearchText)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        int count = 0;
        for (String item : right) {
            if (normalizedLeft.contains(normalizeSearchText(item))) {
                count += 1;
            }
        }
        return count;
    }

    private List<Map<String, Object>> sortViews(List<Map<String, Object>> views, String sortKey) {
        List<Map<String, Object>> sorted = new ArrayList<>(views);
        Comparator<Map<String, Object>> publishedAtComparator = Comparator
                .comparing((Map<String, Object> item) -> asDateTime(item.get("publishedAt")),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed();

        Comparator<Map<String, Object>> comparator = publishedAtComparator;
        if ("salary".equals(sortKey)) {
            comparator = Comparator
                    .comparing((Map<String, Object> item) -> ((Number) item.getOrDefault("salaryMax", 0)).intValue())
                    .reversed()
                    .thenComparing(publishedAtComparator);
        }

        sorted.sort(comparator);
        return sorted;
    }

    private String normalizeSortKey(String sortKey) {
        if ("salary".equalsIgnoreCase(sortKey)) {
            return "salary";
        }
        if ("latest".equalsIgnoreCase(sortKey)) {
            return "latest";
        }
        return "default";
    }

    private LocalDateTime asDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        return null;
    }

    private PageResponse<Map<String, Object>> paginate(List<Map<String, Object>> items, Long pageNum, Long pageSize) {
        return paginateItems(items, pageNum, pageSize);
    }

    private <T> PageResponse<T> paginateItems(List<T> items, Long pageNum, Long pageSize) {
        long current = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long size = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int fromIndex = (int) ((current - 1) * size);
        if (fromIndex >= items.size()) {
            return PageResponse.<T>builder()
                    .pageNum(current)
                    .pageSize(size)
                    .total(items.size())
                    .records(List.of())
                    .build();
        }
        int toIndex = (int) Math.min(fromIndex + size, items.size());
        return PageResponse.<T>builder()
                .pageNum(current)
                .pageSize(size)
                .total(items.size())
                .records(items.subList(fromIndex, toIndex))
                .build();
    }

    private Map<String, Object> buildBaseJobView(JobPost job, Long currentUserId, Set<Long> recentAppliedJobIds,
                                                 Set<Long> favoriteJobIds) {
        return buildBaseJobView(job, currentUserId, recentAppliedJobIds, favoriteJobIds, resolveCompanyName(job.getCompanyUserId()));
    }

    private Map<String, Object> buildBaseJobView(JobPost job, Long currentUserId, Set<Long> recentAppliedJobIds,
                                                 Set<Long> favoriteJobIds, String companyName) {
        LinkedHashMap<String, Object> view = new LinkedHashMap<>();
        view.put("id", job.getId());
        view.put("jobCode", job.getJobCode());
        view.put("title", job.getTitle());
        view.put("category", job.getCategory());
        view.put("location", job.getLocation());
        view.put("salaryMin", job.getSalaryMin());
        view.put("salaryMax", job.getSalaryMax());
        view.put("experienceRequirement", job.getExperienceRequirement());
        view.put("educationRequirement", job.getEducationRequirement());
        view.put("headcount", job.getHeadcount());
        view.put("description", job.getDescription());
        view.put("skillTags", readTagList(job.getSkillTagsJson()));
        view.put("benefitTags", readTagList(job.getBenefitTagsJson()));
        view.put("status", normalizeStatus(job));
        view.put("publishedAt", job.getPublishedAt());
        view.put("expireAt", job.getExpireAt());
        view.put("companyUserId", job.getCompanyUserId());
        view.put("companyName", safe(companyName));
        view.put("recentlyApplied", currentUserId != null && recentAppliedJobIds.contains(job.getId()));
        view.put("favorited", currentUserId != null && favoriteJobIds.contains(job.getId()));
        return view;
    }

    private Map<Long, String> getCompanyNamesByUserIds(Set<Long> companyUserIds) {
        if (companyUserIds == null || companyUserIds.isEmpty()) {
            return Map.of();
        }
        return companyProfileMapper.selectList(new LambdaQueryWrapper<CompanyProfile>()
                        .in(CompanyProfile::getUserId, companyUserIds))
                .stream()
                .collect(Collectors.toMap(CompanyProfile::getUserId, CompanyProfile::getCompanyName, (left, right) -> left));
    }

    private String resolveCompanyName(Long companyUserId) {
        if (companyUserId == null) {
            return "";
        }
        CompanyProfile company = companyProfileMapper.selectOne(new LambdaQueryWrapper<CompanyProfile>()
                .eq(CompanyProfile::getUserId, companyUserId));
        return company == null ? "" : safe(company.getCompanyName());
    }

    private Set<Long> getRecentAppliedJobIds(Long currentUserId) {
        if (currentUserId == null) {
            return Set.of();
        }
        LocalDateTime duplicateDeadline = LocalDateTime.now().minusDays(7);
        return jobApplicationMapper.selectList(new LambdaQueryWrapper<JobApplication>()
                        .eq(JobApplication::getJobseekerUserId, currentUserId)
                        .ge(JobApplication::getAppliedAt, duplicateDeadline)
                        .select(JobApplication::getJobId))
                .stream()
                .map(JobApplication::getJobId)
                .collect(Collectors.toSet());
    }

    private Set<Long> getFavoriteJobIds(Long currentUserId) {
        if (currentUserId == null) {
            return Set.of();
        }
        return jobFavoriteMapper.selectList(new LambdaQueryWrapper<JobFavorite>()
                        .eq(JobFavorite::getJobseekerUserId, currentUserId)
                        .select(JobFavorite::getJobId))
                .stream()
                .map(JobFavorite::getJobId)
                .collect(Collectors.toSet());
    }

    private String normalizeStatus(JobPost job) {
        if (JobStatus.PUBLISHED.name().equals(job.getStatus())
                && job.getExpireAt() != null
                && job.getExpireAt().isBefore(LocalDateTime.now())) {
            return JobStatus.EXPIRED.name();
        }
        return job.getStatus();
    }

    private void refreshExpiredJobs() {
        List<JobPost> publishedJobs = jobPostMapper.selectList(new LambdaQueryWrapper<JobPost>()
                .eq(JobPost::getStatus, JobStatus.PUBLISHED.name())
                .and(wrapper -> wrapper.isNull(JobPost::getDeletedFlag).or().eq(JobPost::getDeletedFlag, 0))
                .isNotNull(JobPost::getExpireAt)
                .lt(JobPost::getExpireAt, LocalDateTime.now()));
        for (JobPost publishedJob : publishedJobs) {
            publishedJob.setStatus(JobStatus.EXPIRED.name());
            jobPostMapper.updateById(publishedJob);
        }
    }

    private Integer parseYears(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        String normalized = normalizeSearchText(rawValue);
        if (normalized.contains("不限") || normalized.contains("应届")) {
            return 0;
        }
        String digits = normalized.replaceAll("[^0-9]", " ").trim();
        if (!StringUtils.hasText(digits)) {
            return null;
        }
        String[] parts = digits.split("\\s+");
        try {
            return Integer.parseInt(parts[0]);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer educationRank(String rawEducation) {
        String normalized = normalizeSearchText(rawEducation);
        if (!StringUtils.hasText(normalized) || normalized.contains("不限")) {
            return 0;
        }
        if (normalized.contains("博士") || normalized.contains("phd")) {
            return 5;
        }
        if (normalized.contains("硕士") || normalized.contains("master")) {
            return 4;
        }
        if (normalized.contains("本科") || normalized.contains("bachelor")) {
            return 3;
        }
        if (normalized.contains("大专") || normalized.contains("associate")) {
            return 2;
        }
        if (normalized.contains("高中") || normalized.contains("中专")) {
            return 1;
        }
        return null;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String normalizeSearchText(String value) {
        return safe(value).trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeOptionalRequirement(String value) {
        String normalized = trimToNull(value);
        if ("不限".equals(normalized)) {
            return null;
        }
        return normalized;
    }

    private AdminDtos.ManagedJobView buildManagedJobView(JobPost job, String companyName) {
        AdminDtos.ManagedJobView view = new AdminDtos.ManagedJobView();
        view.setId(job.getId());
        view.setJobCode(job.getJobCode());
        view.setTitle(job.getTitle());
        view.setCategory(job.getCategory());
        view.setLocation(job.getLocation());
        view.setSalaryMin(job.getSalaryMin());
        view.setSalaryMax(job.getSalaryMax());
        view.setExperienceRequirement(job.getExperienceRequirement());
        view.setEducationRequirement(job.getEducationRequirement());
        view.setStatus(normalizeStatus(job));
        view.setPublishedAt(job.getPublishedAt());
        view.setCreatedAt(job.getCreatedAt());
        view.setCompanyUserId(job.getCompanyUserId());
        view.setCompanyName(safe(companyName));
        return view;
    }

    private boolean matchesAdminJobKeyword(String keyword, AdminDtos.ManagedJobView view) {
        String normalizedKeyword = normalizeSearchText(keyword);
        if (!StringUtils.hasText(normalizedKeyword)) {
            return true;
        }
        return containsKeyword(view.getTitle(), normalizedKeyword)
                || containsKeyword(view.getJobCode(), normalizedKeyword)
                || containsKeyword(view.getCategory(), normalizedKeyword)
                || containsKeyword(view.getLocation(), normalizedKeyword);
    }

    private boolean matchesAdminCompanyKeyword(String companyKeyword, String companyName) {
        String normalizedKeyword = normalizeSearchText(companyKeyword);
        if (!StringUtils.hasText(normalizedKeyword)) {
            return true;
        }
        return containsKeyword(companyName, normalizedKeyword);
    }

    private boolean matchesAdminJobStatus(String status, String currentStatus) {
        if (!StringUtils.hasText(status)) {
            return true;
        }
        return safe(currentStatus).equalsIgnoreCase(status.trim());
    }

    private boolean matchesAdminJobCategory(String category, String currentCategory) {
        String normalizedCategory = normalizeSearchText(category);
        if (!StringUtils.hasText(normalizedCategory)) {
            return true;
        }
        return containsKeyword(currentCategory, normalizedCategory);
    }

    private String normalizeModerationAction(String action) {
        String normalizedAction = safe(action).trim().toUpperCase(Locale.ROOT);
        if ("OFFLINE".equals(normalizedAction) || "DELETE".equals(normalizedAction)) {
            return normalizedAction;
        }
        throw new BusinessException("不支持的岗位处理动作");
    }

    private void recordAdminAction(String targetType, Long targetId, String actionType, String reason,
                                   Long operatorUserId, Map<String, Object> metadata) {
        AdminActionLog actionLog = new AdminActionLog();
        actionLog.setTargetType(targetType);
        actionLog.setTargetId(targetId);
        actionLog.setActionType(actionType);
        actionLog.setReason(reason);
        actionLog.setOperatorUserId(operatorUserId);
        actionLog.setMetadataJson(writeMetadata(metadata));
        actionLog.setCreatedAt(LocalDateTime.now());
        adminActionLogMapper.insert(actionLog);
    }

    private String writeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private record JobSearchCriteria(
            String keyword,
            String category,
            String location,
            Integer salaryMin,
            Integer salaryMax,
            String experienceRequirement,
            Integer experienceYears,
            String educationRequirement,
            List<String> benefitTags,
            String sortKey
    ) {
        private static JobSearchCriteria empty() {
            return new JobSearchCriteria(null, null, null, null, null, null, null, null, List.of(), "default");
        }
    }
}
