package com.bishe.recruitment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bishe.recruitment.common.BusinessException;
import com.bishe.recruitment.common.PageResponse;
import com.bishe.recruitment.dto.JobDtos;
import com.bishe.recruitment.entity.CompanyProfile;
import com.bishe.recruitment.entity.JobApplication;
import com.bishe.recruitment.entity.JobPost;
import com.bishe.recruitment.entity.JobseekerProfile;
import com.bishe.recruitment.entity.Resume;
import com.bishe.recruitment.enums.CompanyAuditStatus;
import com.bishe.recruitment.enums.JobStatus;
import com.bishe.recruitment.enums.NotificationType;
import com.bishe.recruitment.mapper.CompanyProfileMapper;
import com.bishe.recruitment.mapper.JobApplicationMapper;
import com.bishe.recruitment.mapper.JobPostMapper;
import com.bishe.recruitment.mapper.JobseekerProfileMapper;
import com.bishe.recruitment.mapper.ResumeMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class JobService {

    private final JobPostMapper jobPostMapper;
    private final CompanyProfileMapper companyProfileMapper;
    private final JobApplicationMapper jobApplicationMapper;
    private final ResumeMapper resumeMapper;
    private final ResumeService resumeService;
    private final JobseekerProfileMapper jobseekerProfileMapper;
    private final NotificationService notificationService;

    public JobService(JobPostMapper jobPostMapper, CompanyProfileMapper companyProfileMapper,
                      JobApplicationMapper jobApplicationMapper, ResumeMapper resumeMapper,
                      ResumeService resumeService, JobseekerProfileMapper jobseekerProfileMapper,
                      NotificationService notificationService) {
        this.jobPostMapper = jobPostMapper;
        this.companyProfileMapper = companyProfileMapper;
        this.jobApplicationMapper = jobApplicationMapper;
        this.resumeMapper = resumeMapper;
        this.resumeService = resumeService;
        this.jobseekerProfileMapper = jobseekerProfileMapper;
        this.notificationService = notificationService;
    }

    @Transactional
    public Map<String, Object> createJob(Long companyUserId, JobDtos.JobSaveRequest request) {
        validateSalary(request);
        JobPost jobPost = new JobPost();
        fillJobFields(jobPost, request);
        jobPost.setCompanyUserId(companyUserId);
        jobPost.setJobCode(generateJobCode());
        jobPost.setStatus(JobStatus.DRAFT.name());
        jobPostMapper.insert(jobPost);
        return buildJobView(jobPost, null);
    }

    @Transactional
    public Map<String, Object> updateJob(Long companyUserId, Long jobId, JobDtos.JobSaveRequest request) {
        validateSalary(request);
        JobPost jobPost = getOwnedJob(companyUserId, jobId);
        fillJobFields(jobPost, request);
        if (JobStatus.PUBLISHED.name().equals(jobPost.getStatus()) && request.getDescription().length() < 200) {
            throw new BusinessException("已发布岗位的岗位描述不能少于200字");
        }
        jobPostMapper.updateById(jobPost);
        return buildJobView(jobPost, null);
    }

    public void deleteJob(Long companyUserId, Long jobId) {
        JobPost jobPost = getOwnedJob(companyUserId, jobId);
        jobPostMapper.deleteById(jobPost.getId());
    }

    public PageResponse<Map<String, Object>> listCompanyJobs(Long companyUserId, String status, long pageNum, long pageSize) {
        Page<JobPost> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<JobPost> wrapper = new LambdaQueryWrapper<JobPost>()
                .eq(JobPost::getCompanyUserId, companyUserId)
                .orderByDesc(JobPost::getCreatedAt);
        if (StringUtils.hasText(status)) {
            wrapper.eq(JobPost::getStatus, status);
        }
        Page<JobPost> result = jobPostMapper.selectPage(page, wrapper);
        List<Map<String, Object>> records = result.getRecords().stream().map(job -> buildJobView(job, null)).toList();
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
        if (targetStatus == JobStatus.EXPIRED && (jobPost.getExpireAt() == null || jobPost.getExpireAt().isAfter(LocalDateTime.now()))) {
            throw new BusinessException("未到过期时间，不能手动设置为已过期");
        }
        jobPost.setStatus(targetStatus.name());
        jobPostMapper.updateById(jobPost);
        return buildJobView(jobPost, null);
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
        return buildJobView(jobPost, null);
    }

    public PageResponse<Map<String, Object>> listJobsForAdmin(String status, long pageNum, long pageSize) {
        Page<JobPost> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<JobPost> wrapper = new LambdaQueryWrapper<JobPost>().orderByDesc(JobPost::getCreatedAt);
        if (StringUtils.hasText(status)) {
            wrapper.eq(JobPost::getStatus, status);
        }
        Page<JobPost> result = jobPostMapper.selectPage(page, wrapper);
        List<Map<String, Object>> records = result.getRecords().stream().map(job -> buildJobView(job, null)).toList();
        return PageResponse.<Map<String, Object>>builder()
                .pageNum(result.getCurrent())
                .pageSize(result.getSize())
                .total(result.getTotal())
                .records(records)
                .build();
    }

    public PageResponse<Map<String, Object>> searchJobs(JobDtos.JobSearchRequest request, Long currentUserId) {
        refreshExpiredJobs();
        Set<Long> recentAppliedJobIds = getRecentAppliedJobIds(currentUserId);
        List<JobPost> jobs = jobPostMapper.selectList(new LambdaQueryWrapper<JobPost>()
                .eq(JobPost::getStatus, JobStatus.PUBLISHED.name())
                .and(wrapper -> wrapper.isNull(JobPost::getExpireAt).or().gt(JobPost::getExpireAt, LocalDateTime.now()))
                .orderByDesc(JobPost::getPublishedAt));
        List<Map<String, Object>> views = jobs.stream()
                .filter(job -> matchesSearch(job, request))
                .map(job -> buildJobView(job, currentUserId, recentAppliedJobIds))
                .toList();
        views = sortViews(views, request.getSortBy(), request.getSortDirection());
        return paginate(views, request.getPageNum(), request.getPageSize());
    }

    public Map<String, Object> getJobDetail(Long jobId, Long currentUserId) {
        refreshExpiredJobs();
        JobPost jobPost = getJobById(jobId);
        if (!JobStatus.PUBLISHED.name().equals(jobPost.getStatus()) && currentUserId == null) {
            throw new BusinessException("岗位不存在或不可查看");
        }
        return buildJobView(jobPost, currentUserId, getRecentAppliedJobIds(currentUserId));
    }

    public List<Map<String, Object>> recommendJobs(Long jobseekerUserId) {
        refreshExpiredJobs();
        Resume resume = resumeService.validateCompleteOrThrow(jobseekerUserId);
        Set<Long> recentAppliedJobIds = getRecentAppliedJobIds(jobseekerUserId);
        List<JobPost> jobs = jobPostMapper.selectList(new LambdaQueryWrapper<JobPost>()
                .eq(JobPost::getStatus, JobStatus.PUBLISHED.name())
                .and(wrapper -> wrapper.isNull(JobPost::getExpireAt).or().gt(JobPost::getExpireAt, LocalDateTime.now())));
        return jobs.stream()
                .map(job -> buildJobView(job, jobseekerUserId, recentAppliedJobIds))
                .filter(item -> ((Number) item.getOrDefault("matchScore", 0)).doubleValue() > 0)
                .sorted(Comparator.comparing((Map<String, Object> item) -> ((Number) item.get("matchScore")).doubleValue()).reversed())
                .limit(10)
                .toList();
    }

    public JobPost getJobById(Long jobId) {
        JobPost jobPost = jobPostMapper.selectById(jobId);
        if (jobPost == null) {
            throw new BusinessException("岗位不存在");
        }
        return jobPost;
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
        jobPost.setExpireAt(request.getExpireAt());
    }

    private void validateSalary(JobDtos.JobSaveRequest request) {
        if (request.getSalaryMin() > request.getSalaryMax()) {
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

    private boolean matchesSearch(JobPost job, JobDtos.JobSearchRequest request) {
        if (StringUtils.hasText(request.getKeyword())) {
            String keyword = request.getKeyword().trim();
            String content = String.join(" ", safe(job.getTitle()), safe(job.getCategory()), safe(job.getLocation()), safe(job.getDescription()));
            if (!content.contains(keyword)) {
                return false;
            }
        }
        if (StringUtils.hasText(request.getCategory()) && !Objects.equals(job.getCategory(), request.getCategory())) {
            return false;
        }
        if (StringUtils.hasText(request.getLocation()) && !safe(job.getLocation()).contains(request.getLocation())) {
            return false;
        }
        if (request.getSalaryMin() != null && job.getSalaryMax() < request.getSalaryMin()) {
            return false;
        }
        if (request.getSalaryMax() != null && job.getSalaryMin() > request.getSalaryMax()) {
            return false;
        }
        if (StringUtils.hasText(request.getEducationRequirement())
                && !safe(job.getEducationRequirement()).contains(request.getEducationRequirement())) {
            return false;
        }
        if (StringUtils.hasText(request.getExperienceRequirement())
                && !safe(job.getExperienceRequirement()).contains(request.getExperienceRequirement())) {
            return false;
        }
        return true;
    }

    private List<Map<String, Object>> sortViews(List<Map<String, Object>> views, String sortBy, String sortDirection) {
        List<Map<String, Object>> sorted = new ArrayList<>(views);
        Comparator<Map<String, Object>> comparator;
        if ("salary".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(item -> ((Number) item.getOrDefault("salaryMax", 0)).intValue());
        } else if ("matchScore".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(item -> ((Number) item.getOrDefault("matchScore", 0)).doubleValue());
        } else {
            comparator = Comparator.comparing(item -> Objects.toString(item.get("publishedAt"), ""));
        }
        if (!"asc".equalsIgnoreCase(sortDirection)) {
            comparator = comparator.reversed();
        }
        sorted.sort(comparator);
        return sorted;
    }

    private PageResponse<Map<String, Object>> paginate(List<Map<String, Object>> items, Long pageNum, Long pageSize) {
        long current = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long size = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int fromIndex = (int) ((current - 1) * size);
        if (fromIndex >= items.size()) {
            return PageResponse.<Map<String, Object>>builder()
                    .pageNum(current)
                    .pageSize(size)
                    .total(items.size())
                    .records(List.of())
                    .build();
        }
        int toIndex = (int) Math.min(fromIndex + size, items.size());
        return PageResponse.<Map<String, Object>>builder()
                .pageNum(current)
                .pageSize(size)
                .total(items.size())
                .records(items.subList(fromIndex, toIndex))
                .build();
    }

    private Map<String, Object> buildJobView(JobPost job, Long currentUserId) {
        return buildJobView(job, currentUserId, Set.of());
    }

    private Map<String, Object> buildJobView(JobPost job, Long currentUserId, Set<Long> recentAppliedJobIds) {
        CompanyProfile company = companyProfileMapper.selectOne(new LambdaQueryWrapper<CompanyProfile>()
                .eq(CompanyProfile::getUserId, job.getCompanyUserId()));
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
        view.put("status", normalizeStatus(job));
        view.put("publishedAt", job.getPublishedAt());
        view.put("expireAt", job.getExpireAt());
        view.put("companyUserId", job.getCompanyUserId());
        view.put("companyName", company == null ? "" : company.getCompanyName());
        view.put("recentlyApplied", currentUserId != null && recentAppliedJobIds.contains(job.getId()));
        if (currentUserId != null) {
            Resume resume = resumeMapper.selectOne(new LambdaQueryWrapper<Resume>().eq(Resume::getUserId, currentUserId));
            JobseekerProfile profile = jobseekerProfileMapper.selectOne(new LambdaQueryWrapper<JobseekerProfile>()
                    .eq(JobseekerProfile::getUserId, currentUserId));
            if (resume != null && profile != null) {
                Map<String, Object> matchData = calculateMatch(job, resume, profile);
                view.putAll(matchData);
            }
        }
        return view;
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

    private String normalizeStatus(JobPost job) {
        if (JobStatus.PUBLISHED.name().equals(job.getStatus()) && job.getExpireAt() != null && job.getExpireAt().isBefore(LocalDateTime.now())) {
            return JobStatus.EXPIRED.name();
        }
        return job.getStatus();
    }

    private Map<String, Object> calculateMatch(JobPost job, Resume resume, JobseekerProfile profile) {
        double total = 0;
        List<String> reasons = new ArrayList<>();
        List<String> skills = resumeService.getResumeSkills(resume.getId());
        String text = String.join(" ", safe(job.getTitle()), safe(job.getCategory()), safe(job.getDescription())).toLowerCase();
        long hitSkillCount = skills.stream().filter(skill -> text.contains(skill.toLowerCase())).count();
        if (!skills.isEmpty()) {
            double skillScore = (double) hitSkillCount / skills.size() * 40;
            total += skillScore;
            if (hitSkillCount > 0) {
                reasons.add("技能匹配 " + hitSkillCount + " 项");
            }
        }
        if (StringUtils.hasText(resume.getExpectedCategory()) && safe(job.getCategory()).contains(resume.getExpectedCategory())) {
            total += 20;
            reasons.add("岗位类别匹配");
        } else if (StringUtils.hasText(profile.getDesiredPositionCategory()) && safe(job.getCategory()).contains(profile.getDesiredPositionCategory())) {
            total += 20;
            reasons.add("岗位类别匹配");
        }
        String preferredCity = StringUtils.hasText(profile.getPreferredCity()) ? profile.getPreferredCity() : resume.getCity();
        if (StringUtils.hasText(preferredCity) && safe(job.getLocation()).contains(preferredCity)) {
            total += 15;
            reasons.add("工作地点匹配");
        }
        Integer expectedMin = resume.getExpectedSalaryMin() != null ? resume.getExpectedSalaryMin() : profile.getExpectedSalaryMin();
        Integer expectedMax = resume.getExpectedSalaryMax() != null ? resume.getExpectedSalaryMax() : profile.getExpectedSalaryMax();
        if (expectedMin != null && expectedMax != null && job.getSalaryMax() >= expectedMin && job.getSalaryMin() <= expectedMax) {
            total += 15;
            reasons.add("薪资范围匹配");
        }
        if (StringUtils.hasText(resume.getHighestEducation()) && safe(job.getEducationRequirement()).contains(resume.getHighestEducation())) {
            total += 5;
            reasons.add("学历要求匹配");
        }
        if (resume.getYearsOfExperience() != null && safe(job.getExperienceRequirement()).contains(String.valueOf(resume.getYearsOfExperience()))) {
            total += 5;
            reasons.add("经验要求匹配");
        } else if (resume.getYearsOfExperience() != null && resume.getYearsOfExperience() >= 1) {
            total += 2;
        }
        return Map.of("matchScore", Math.round(total * 10.0) / 10.0, "matchReasons", reasons);
    }

    private void refreshExpiredJobs() {
        List<JobPost> publishedJobs = jobPostMapper.selectList(new LambdaQueryWrapper<JobPost>()
                .eq(JobPost::getStatus, JobStatus.PUBLISHED.name())
                .isNotNull(JobPost::getExpireAt)
                .lt(JobPost::getExpireAt, LocalDateTime.now()));
        for (JobPost publishedJob : publishedJobs) {
            publishedJob.setStatus(JobStatus.EXPIRED.name());
            jobPostMapper.updateById(publishedJob);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
