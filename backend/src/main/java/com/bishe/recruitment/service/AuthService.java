package com.bishe.recruitment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bishe.recruitment.common.BusinessException;
import com.bishe.recruitment.dto.AuthDtos;
import com.bishe.recruitment.entity.CompanyProfile;
import com.bishe.recruitment.entity.JobseekerProfile;
import com.bishe.recruitment.entity.Resume;
import com.bishe.recruitment.entity.SysUser;
import com.bishe.recruitment.enums.CompanyAuditStatus;
import com.bishe.recruitment.enums.NotificationType;
import com.bishe.recruitment.enums.UserRole;
import com.bishe.recruitment.mapper.CompanyProfileMapper;
import com.bishe.recruitment.mapper.JobseekerProfileMapper;
import com.bishe.recruitment.mapper.ResumeMapper;
import com.bishe.recruitment.security.AuthenticatedUser;
import com.bishe.recruitment.security.CustomUserDetailsService;
import com.bishe.recruitment.security.JwtUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserSupportService userSupportService;
    private final CompanyProfileMapper companyProfileMapper;
    private final JobseekerProfileMapper jobseekerProfileMapper;
    private final ResumeMapper resumeMapper;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtUtils jwtUtils;
    private final NotificationService notificationService;

    public AuthService(UserSupportService userSupportService, CompanyProfileMapper companyProfileMapper,
                       JobseekerProfileMapper jobseekerProfileMapper, ResumeMapper resumeMapper,
                       CustomUserDetailsService customUserDetailsService, JwtUtils jwtUtils,
                       NotificationService notificationService) {
        this.userSupportService = userSupportService;
        this.companyProfileMapper = companyProfileMapper;
        this.jobseekerProfileMapper = jobseekerProfileMapper;
        this.resumeMapper = resumeMapper;
        this.customUserDetailsService = customUserDetailsService;
        this.jwtUtils = jwtUtils;
        this.notificationService = notificationService;
    }

    public Map<String, Object> registerCompany(AuthDtos.CompanyRegisterRequest request) {
        SysUser user = userSupportService.createUser(
                request.getEmail(),
                request.getPhone(),
                request.getPassword(),
                request.getCompanyName(),
                UserRole.COMPANY);
        CompanyProfile profile = new CompanyProfile();
        profile.setUserId(user.getId());
        profile.setCompanyName(request.getCompanyName());
        profile.setUnifiedSocialCreditCode(request.getUnifiedSocialCreditCode());
        profile.setContactPerson(request.getContactPerson());
        profile.setPhone(request.getPhone());
        profile.setEmail(request.getEmail());
        profile.setAuditStatus(CompanyAuditStatus.PENDING.name());
        companyProfileMapper.insert(profile);

        notifyAdminsForCompanyAudit(request.getCompanyName(), "提交了企业认证申请，请尽快审核。");
        return Map.of("userId", user.getId(), "auditStatus", profile.getAuditStatus());
    }

    public Map<String, Object> registerJobseeker(AuthDtos.JobseekerRegisterRequest request) {
        SysUser user = userSupportService.createUser(
                request.getEmail(),
                request.getPhone(),
                request.getPassword(),
                request.getFullName(),
                UserRole.JOBSEEKER);
        JobseekerProfile profile = new JobseekerProfile();
        profile.setUserId(user.getId());
        profile.setFullName(request.getFullName());
        profile.setPhone(request.getPhone());
        profile.setEmail(request.getEmail());
        jobseekerProfileMapper.insert(profile);

        Resume resume = new Resume();
        resume.setUserId(user.getId());
        resume.setTemplateCode("classic");
        resume.setFullName(request.getFullName());
        resume.setPhone(request.getPhone());
        resume.setEmail(request.getEmail());
        resume.setCompletenessScore(20);
        resumeMapper.insert(resume);
        return Map.of("userId", user.getId());
    }

    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        AuthenticatedUser user = customUserDetailsService.loadByAccount(request.getAccount());
        if (!userSupportService.passwordEncoder().matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "账号或密码错误");
        }
        AuthDtos.LoginResponse response = new AuthDtos.LoginResponse();
        response.setUserId(user.getUserId());
        response.setToken(jwtUtils.generateToken(user));
        response.setRole(user.getRoles().isEmpty() ? "" : user.getRoles().getFirst());
        response.setDisplayName(user.getDisplayName());
        return response;
    }

    public Map<String, Object> currentProfile(Long userId) {
        SysUser user = userSupportService.getUserById(userId);
        List<String> roles = userSupportService.getRoles(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", user.getId());
        result.put("displayName", user.getDisplayName());
        result.put("email", user.getEmail());
        result.put("phone", user.getPhone());
        result.put("roles", roles);
        result.put("primaryRole", roles.isEmpty() ? "" : roles.getFirst());
        result.put("workspaceLabel", buildWorkspaceLabel(roles));

        if (roles.contains(UserRole.COMPANY.name())) {
            result.put(
                    "companyProfile",
                    companyProfileMapper.selectOne(new LambdaQueryWrapper<CompanyProfile>()
                            .eq(CompanyProfile::getUserId, userId)));
        }
        if (roles.contains(UserRole.JOBSEEKER.name())) {
            result.put(
                    "jobseekerProfile",
                    jobseekerProfileMapper.selectOne(new LambdaQueryWrapper<JobseekerProfile>()
                            .eq(JobseekerProfile::getUserId, userId)));
        }
        return result;
    }

    @Transactional
    public Map<String, Object> updateJobseekerProfile(Long userId, AuthDtos.JobseekerProfileUpdateRequest request) {
        userSupportService.ensureAccountUnique(request.getEmail(), request.getPhone(), userId);

        SysUser user = userSupportService.getUserById(userId);
        JobseekerProfile profile = userSupportService.getJobseekerProfileByUserId(userId);
        if (profile == null) {
            throw new BusinessException("求职者资料不存在");
        }

        user.setUsername(request.getEmail());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setDisplayName(request.getFullName());
        userSupportService.updateUser(user);

        profile.setFullName(request.getFullName());
        profile.setPhone(request.getPhone());
        profile.setEmail(request.getEmail());
        jobseekerProfileMapper.updateById(profile);

        return currentProfile(userId);
    }

    @Transactional
    public Map<String, Object> updateCompanyProfile(Long userId, AuthDtos.CompanyProfileUpdateRequest request) {
        userSupportService.ensureAccountUnique(request.getEmail(), request.getPhone(), userId);
        ensureCompanyCreditCodeUnique(request.getUnifiedSocialCreditCode(), userId);

        SysUser user = userSupportService.getUserById(userId);
        CompanyProfile profile = userSupportService.getCompanyProfileByUserId(userId);
        if (profile == null) {
            throw new BusinessException("企业资料不存在");
        }

        boolean coreChanged = !Objects.equals(profile.getCompanyName(), request.getCompanyName())
                || !Objects.equals(profile.getUnifiedSocialCreditCode(), request.getUnifiedSocialCreditCode());

        user.setUsername(request.getEmail());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setDisplayName(request.getCompanyName());
        userSupportService.updateUser(user);

        profile.setCompanyName(request.getCompanyName());
        profile.setUnifiedSocialCreditCode(request.getUnifiedSocialCreditCode());
        profile.setContactPerson(request.getContactPerson());
        profile.setPhone(request.getPhone());
        profile.setEmail(request.getEmail());
        profile.setAddress(request.getAddress());
        profile.setDescription(request.getDescription());
        if (coreChanged) {
            profile.setAuditStatus(CompanyAuditStatus.PENDING.name());
        }
        companyProfileMapper.updateById(profile);

        if (coreChanged) {
            notifyAdminsForCompanyAudit(request.getCompanyName(), "更新了企业认证核心资料，请重新审核。");
        }

        return currentProfile(userId);
    }

    @Transactional
    public void changePassword(Long userId, AuthDtos.ChangePasswordRequest request) {
        if (!Objects.equals(request.getNewPassword(), request.getConfirmPassword())) {
            throw new BusinessException("两次输入的新密码不一致");
        }

        SysUser user = userSupportService.getUserById(userId);
        if (!userSupportService.passwordEncoder().matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("当前密码不正确");
        }
        if (userSupportService.passwordEncoder().matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException("新密码不能与当前密码相同");
        }

        user.setPassword(userSupportService.passwordEncoder().encode(request.getNewPassword()));
        userSupportService.updateUser(user);
    }

    private void ensureCompanyCreditCodeUnique(String unifiedSocialCreditCode, Long userId) {
        long duplicateCount = companyProfileMapper.selectCount(new LambdaQueryWrapper<CompanyProfile>()
                .eq(CompanyProfile::getUnifiedSocialCreditCode, unifiedSocialCreditCode)
                .ne(CompanyProfile::getUserId, userId));
        if (duplicateCount > 0) {
            throw new BusinessException("统一社会信用代码已被使用");
        }
    }

    private void notifyAdminsForCompanyAudit(String companyName, String suffix) {
        List<Long> adminIds = userSupportService.listUserIdsByRole(UserRole.ADMIN);
        adminIds.forEach(adminId -> notificationService.createAndPush(
                adminId,
                NotificationType.COMPANY_AUDIT,
                "企业认证待审核",
                companyName + suffix));
    }

    private String buildWorkspaceLabel(List<String> roles) {
        if (roles.contains(UserRole.ADMIN.name())) {
            return "管理员工作区";
        }
        if (roles.contains(UserRole.COMPANY.name())) {
            return "企业招聘工作区";
        }
        if (roles.contains(UserRole.JOBSEEKER.name())) {
            return "求职者工作区";
        }
        return "系统工作区";
    }
}
