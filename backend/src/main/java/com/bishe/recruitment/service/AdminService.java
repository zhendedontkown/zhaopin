package com.bishe.recruitment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bishe.recruitment.common.BusinessException;
import com.bishe.recruitment.common.PageResponse;
import com.bishe.recruitment.dto.AdminDtos;
import com.bishe.recruitment.entity.AdminActionLog;
import com.bishe.recruitment.entity.CompanyProfile;
import com.bishe.recruitment.entity.JobApplication;
import com.bishe.recruitment.entity.JobPost;
import com.bishe.recruitment.entity.JobseekerProfile;
import com.bishe.recruitment.entity.SysRole;
import com.bishe.recruitment.entity.SysUser;
import com.bishe.recruitment.entity.SysUserRole;
import com.bishe.recruitment.enums.ApplicationStatus;
import com.bishe.recruitment.enums.CompanyAuditStatus;
import com.bishe.recruitment.enums.NotificationType;
import com.bishe.recruitment.enums.UserAccountStatus;
import com.bishe.recruitment.enums.UserRole;
import com.bishe.recruitment.mapper.AdminActionLogMapper;
import com.bishe.recruitment.mapper.CompanyProfileMapper;
import com.bishe.recruitment.mapper.JobApplicationMapper;
import com.bishe.recruitment.mapper.JobPostMapper;
import com.bishe.recruitment.mapper.JobseekerProfileMapper;
import com.bishe.recruitment.mapper.SysRoleMapper;
import com.bishe.recruitment.mapper.SysUserMapper;
import com.bishe.recruitment.mapper.SysUserRoleMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
public class AdminService {

    private static final String TARGET_TYPE_USER = "USER";
    private static final String TARGET_TYPE_COMPANY_AUDIT = "COMPANY_AUDIT";

    private final CompanyProfileMapper companyProfileMapper;
    private final JobseekerProfileMapper jobseekerProfileMapper;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final JobPostMapper jobPostMapper;
    private final JobApplicationMapper jobApplicationMapper;
    private final NotificationService notificationService;
    private final AdminActionLogMapper adminActionLogMapper;
    private final ObjectMapper objectMapper;

    public AdminService(CompanyProfileMapper companyProfileMapper,
                        JobseekerProfileMapper jobseekerProfileMapper,
                        SysUserMapper sysUserMapper,
                        SysRoleMapper sysRoleMapper,
                        SysUserRoleMapper sysUserRoleMapper,
                        JobPostMapper jobPostMapper,
                        JobApplicationMapper jobApplicationMapper,
                        NotificationService notificationService,
                        AdminActionLogMapper adminActionLogMapper,
                        ObjectMapper objectMapper) {
        this.companyProfileMapper = companyProfileMapper;
        this.jobseekerProfileMapper = jobseekerProfileMapper;
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.jobPostMapper = jobPostMapper;
        this.jobApplicationMapper = jobApplicationMapper;
        this.notificationService = notificationService;
        this.adminActionLogMapper = adminActionLogMapper;
        this.objectMapper = objectMapper;
    }

    public PageResponse<AdminDtos.CompanyUserView> listCompanyUsers(String keyword,
                                                                    String auditStatus,
                                                                    String userStatus,
                                                                    long pageNum,
                                                                    long pageSize) {
        List<AdminDtos.CompanyUserView> records = loadCompanyUserViews().stream()
                .filter(view -> matchesKeyword(keyword,
                        view.getCompanyName(),
                        view.getDisplayName(),
                        view.getContactPerson(),
                        view.getPhone(),
                        view.getEmail(),
                        view.getUnifiedSocialCreditCode()))
                .filter(view -> matchesEnumFilter(auditStatus, view.getAuditStatus()))
                .filter(view -> matchesUserStatusFilter(userStatus, view.getUserStatus()))
                .toList();
        return paginate(records, pageNum, pageSize);
    }

    public PageResponse<AdminDtos.JobseekerUserView> listJobseekers(String keyword,
                                                                    String userStatus,
                                                                    long pageNum,
                                                                    long pageSize) {
        List<AdminDtos.JobseekerUserView> records = loadJobseekerUserViews().stream()
                .filter(view -> matchesKeyword(keyword,
                        view.getDisplayName(),
                        view.getFullName(),
                        view.getPhone(),
                        view.getEmail(),
                        view.getHighestEducation(),
                        view.getDesiredPositionCategory()))
                .filter(view -> matchesUserStatusFilter(userStatus, view.getUserStatus()))
                .toList();
        return paginate(records, pageNum, pageSize);
    }

    public PageResponse<AdminDtos.CompanyUserView> listCompanyAudits(String keyword,
                                                                     String auditStatus,
                                                                     String userStatus,
                                                                     long pageNum,
                                                                     long pageSize) {
        List<AdminDtos.CompanyUserView> records = loadCompanyUserViews().stream()
                .filter(view -> matchesKeyword(keyword,
                        view.getCompanyName(),
                        view.getContactPerson(),
                        view.getPhone(),
                        view.getEmail(),
                        view.getUnifiedSocialCreditCode()))
                .filter(view -> matchesEnumFilter(auditStatus, view.getAuditStatus()))
                .filter(view -> matchesUserStatusFilter(userStatus, view.getUserStatus()))
                .toList();
        return paginate(records, pageNum, pageSize);
    }

    @Transactional
    public Map<String, Object> updateUserStatus(Long userId, String status, String reason, Long operatorUserId) {
        UserAccountStatus targetStatus = normalizeUserStatus(status);
        SysUser user = getUserOrThrow(userId);
        UserAccountStatus currentStatus = resolveUserStatus(user.getStatus());

        if (loadUserRoleCodes(userId).contains(UserRole.ADMIN.name())) {
            throw new BusinessException("不能修改管理员账号状态");
        }
        if (currentStatus == targetStatus) {
            return Map.of("userId", userId, "status", currentStatus.name());
        }
        if (targetStatus != UserAccountStatus.ACTIVE) {
            requireReason(reason, "请填写操作原因");
        }

        user.setStatus(targetStatus.name());
        sysUserMapper.updateById(user);

        recordAdminAction(
                TARGET_TYPE_USER,
                userId,
                targetStatus.name(),
                trimToNull(reason),
                operatorUserId,
                metadataOf(
                        "previousStatus", currentStatus.name(),
                        "currentStatus", targetStatus.name(),
                        "displayName", user.getDisplayName()));

        return Map.of("userId", userId, "status", targetStatus.name());
    }

    @Transactional
    public AdminDtos.CompanyUserView auditCompany(Long companyUserId, String auditStatus, String reason, Long operatorUserId) {
        CompanyAuditStatus status = normalizeCompanyAuditStatus(auditStatus);
        CompanyProfile profile = companyProfileMapper.selectOne(new LambdaQueryWrapper<CompanyProfile>()
                .eq(CompanyProfile::getUserId, companyUserId));
        if (profile == null) {
            throw new BusinessException("企业不存在");
        }
        if (status == CompanyAuditStatus.REJECTED) {
            requireReason(reason, "驳回时必须填写原因");
        }

        CompanyAuditStatus previousStatus = normalizeCompanyAuditStatus(profile.getAuditStatus());
        if (previousStatus != status) {
            profile.setAuditStatus(status.name());
            companyProfileMapper.updateById(profile);

            String content = status == CompanyAuditStatus.APPROVED
                    ? "您的企业认证已审核通过，可正常发布招聘岗位。"
                    : "您的企业认证已被驳回，原因：" + trimToNull(reason) + "。请完善资料后重新提交。";
            notificationService.createAndPush(
                    companyUserId,
                    NotificationType.COMPANY_AUDIT,
                    "企业认证状态已更新",
                    content);

            recordAdminAction(
                    TARGET_TYPE_COMPANY_AUDIT,
                    companyUserId,
                    status.name(),
                    trimToNull(reason),
                    operatorUserId,
                    metadataOf(
                            "previousAuditStatus", previousStatus.name(),
                            "currentAuditStatus", status.name(),
                            "companyName", profile.getCompanyName()));
        }

        SysUser user = sysUserMapper.selectById(companyUserId);
        return toCompanyUserView(profile, user);
    }

    public AdminDtos.DashboardView dashboard() {
        List<AdminDtos.CompanyUserView> companyUsers = loadCompanyUserViews();
        List<JobApplication> applications = jobApplicationMapper.selectList(null);

        AdminDtos.DashboardView view = new AdminDtos.DashboardView();
        view.setUserCount(sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .ne(SysUser::getStatus, UserAccountStatus.DELETED.name())));
        view.setCompanyUserCount(companyUsers.stream()
                .filter(item -> !UserAccountStatus.DELETED.name().equals(item.getUserStatus()))
                .count());
        view.setJobseekerUserCount(loadJobseekerUserViews().stream()
                .filter(item -> !UserAccountStatus.DELETED.name().equals(item.getUserStatus()))
                .count());
        view.setPendingCompanyAuditCount(companyUsers.stream()
                .filter(item -> !UserAccountStatus.DELETED.name().equals(item.getUserStatus()))
                .filter(item -> CompanyAuditStatus.PENDING.name().equals(item.getAuditStatus()))
                .count());
        view.setJobCount(jobPostMapper.selectCount(notDeletedJobWrapper()));
        view.setApplicationCount(applications.size());
        view.setInterviewingCount(countApplicationsByStatus(applications, ApplicationStatus.INTERVIEWING));
        view.setOfferedCount(countApplicationsByStatus(applications, ApplicationStatus.OFFERED));
        view.setRejectedCount(countApplicationsByStatus(applications, ApplicationStatus.REJECTED));
        return view;
    }

    private long countApplicationsByStatus(List<JobApplication> applications, ApplicationStatus targetStatus) {
        return applications.stream()
                .filter(application -> {
                    try {
                        return ApplicationStatus.fromValue(application.getStatus()) == targetStatus;
                    } catch (IllegalArgumentException ex) {
                        return false;
                    }
                })
                .count();
    }

    private List<AdminDtos.CompanyUserView> loadCompanyUserViews() {
        List<CompanyProfile> profiles = companyProfileMapper.selectList(new LambdaQueryWrapper<CompanyProfile>()
                .orderByDesc(CompanyProfile::getCreatedAt));
        Map<Long, SysUser> userMap = loadUserMap(profiles.stream().map(CompanyProfile::getUserId).toList());
        return profiles.stream()
                .map(profile -> toCompanyUserView(profile, userMap.get(profile.getUserId())))
                .filter(Objects::nonNull)
                .toList();
    }

    private List<AdminDtos.JobseekerUserView> loadJobseekerUserViews() {
        List<JobseekerProfile> profiles = jobseekerProfileMapper.selectList(new LambdaQueryWrapper<JobseekerProfile>()
                .orderByDesc(JobseekerProfile::getCreatedAt));
        Map<Long, SysUser> userMap = loadUserMap(profiles.stream().map(JobseekerProfile::getUserId).toList());
        return profiles.stream()
                .map(profile -> toJobseekerUserView(profile, userMap.get(profile.getUserId())))
                .filter(Objects::nonNull)
                .toList();
    }

    private Map<Long, SysUser> loadUserMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, item -> item, (left, right) -> left));
    }

    private AdminDtos.CompanyUserView toCompanyUserView(CompanyProfile profile, SysUser user) {
        if (profile == null || user == null) {
            return null;
        }
        AdminDtos.CompanyUserView view = new AdminDtos.CompanyUserView();
        view.setUserId(profile.getUserId());
        view.setDisplayName(user.getDisplayName());
        view.setCompanyName(profile.getCompanyName());
        view.setContactPerson(profile.getContactPerson());
        view.setPhone(profile.getPhone());
        view.setEmail(profile.getEmail());
        view.setUnifiedSocialCreditCode(profile.getUnifiedSocialCreditCode());
        view.setAuditStatus(profile.getAuditStatus());
        view.setUserStatus(resolveUserStatus(user.getStatus()).name());
        view.setCreatedAt(profile.getCreatedAt());
        return view;
    }

    private AdminDtos.JobseekerUserView toJobseekerUserView(JobseekerProfile profile, SysUser user) {
        if (profile == null || user == null) {
            return null;
        }
        AdminDtos.JobseekerUserView view = new AdminDtos.JobseekerUserView();
        view.setUserId(profile.getUserId());
        view.setDisplayName(user.getDisplayName());
        view.setFullName(profile.getFullName());
        view.setPhone(profile.getPhone());
        view.setEmail(profile.getEmail());
        view.setHighestEducation(profile.getHighestEducation());
        view.setDesiredPositionCategory(profile.getDesiredPositionCategory());
        view.setUserStatus(resolveUserStatus(user.getStatus()).name());
        view.setCreatedAt(profile.getCreatedAt());
        return view;
    }

    private Set<String> loadUserRoleCodes(Long userId) {
        List<Long> roleIds = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .toList();
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        return sysRoleMapper.selectBatchIds(roleIds).stream()
                .map(SysRole::getCode)
                .collect(Collectors.toSet());
    }

    private SysUser getUserOrThrow(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private UserAccountStatus normalizeUserStatus(String status) {
        try {
            return UserAccountStatus.fromValue(status);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("不支持的账号状态");
        }
    }

    private UserAccountStatus resolveUserStatus(String status) {
        try {
            return UserAccountStatus.fromValue(status);
        } catch (IllegalArgumentException ex) {
            return UserAccountStatus.ACTIVE;
        }
    }

    private CompanyAuditStatus normalizeCompanyAuditStatus(String auditStatus) {
        try {
            return CompanyAuditStatus.valueOf(StringUtils.hasText(auditStatus)
                    ? auditStatus.trim().toUpperCase(Locale.ROOT)
                    : CompanyAuditStatus.PENDING.name());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("不支持的审核状态");
        }
    }

    private void requireReason(String reason, String message) {
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException(message);
        }
    }

    private boolean matchesUserStatusFilter(String expectedStatus, String actualStatus) {
        if (!StringUtils.hasText(expectedStatus)) {
            return !UserAccountStatus.DELETED.name().equals(actualStatus);
        }
        return matchesEnumFilter(expectedStatus, actualStatus);
    }

    private boolean matchesEnumFilter(String expectedValue, String actualValue) {
        if (!StringUtils.hasText(expectedValue)) {
            return true;
        }
        return safe(actualValue).equalsIgnoreCase(expectedValue.trim());
    }

    private boolean matchesKeyword(String keyword, String... fields) {
        String normalizedKeyword = normalize(keyword);
        if (!StringUtils.hasText(normalizedKeyword)) {
            return true;
        }
        for (String field : fields) {
            if (normalize(field).contains(normalizedKeyword)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return safe(value).trim().toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private <T> PageResponse<T> paginate(List<T> records, long pageNum, long pageSize) {
        long current = pageNum < 1 ? 1 : pageNum;
        long size = pageSize < 1 ? 10 : pageSize;
        int fromIndex = (int) ((current - 1) * size);
        if (fromIndex >= records.size()) {
            return PageResponse.<T>builder()
                    .pageNum(current)
                    .pageSize(size)
                    .total(records.size())
                    .records(List.of())
                    .build();
        }
        int toIndex = Math.min(records.size(), fromIndex + (int) size);
        return PageResponse.<T>builder()
                .pageNum(current)
                .pageSize(size)
                .total(records.size())
                .records(records.subList(fromIndex, toIndex))
                .build();
    }

    private LambdaQueryWrapper<JobPost> notDeletedJobWrapper() {
        return new LambdaQueryWrapper<JobPost>()
                .and(wrapper -> wrapper.isNull(JobPost::getDeletedFlag).or().eq(JobPost::getDeletedFlag, 0));
    }

    private void recordAdminAction(String targetType,
                                   Long targetId,
                                   String actionType,
                                   String reason,
                                   Long operatorUserId,
                                   Map<String, Object> metadata) {
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

    private Map<String, Object> metadataOf(Object... entries) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        List<Object> values = new ArrayList<>(List.of(entries));
        for (int index = 0; index + 1 < values.size(); index += 2) {
            Object key = values.get(index);
            Object value = values.get(index + 1);
            if (key instanceof String keyString && value != null) {
                metadata.put(keyString, value);
            }
        }
        return metadata;
    }
}
