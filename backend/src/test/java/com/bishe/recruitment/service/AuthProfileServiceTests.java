package com.bishe.recruitment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bishe.recruitment.common.BusinessException;
import com.bishe.recruitment.dto.AuthDtos;
import com.bishe.recruitment.entity.CompanyProfile;
import com.bishe.recruitment.entity.JobseekerProfile;
import com.bishe.recruitment.entity.Notification;
import com.bishe.recruitment.entity.Resume;
import com.bishe.recruitment.entity.SysUser;
import com.bishe.recruitment.enums.CompanyAuditStatus;
import com.bishe.recruitment.enums.NotificationType;
import com.bishe.recruitment.mapper.CompanyProfileMapper;
import com.bishe.recruitment.mapper.JobseekerProfileMapper;
import com.bishe.recruitment.mapper.NotificationMapper;
import com.bishe.recruitment.mapper.ResumeMapper;
import com.bishe.recruitment.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AuthProfileServiceTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private ResumeMapper resumeMapper;

    @Autowired
    private JobseekerProfileMapper jobseekerProfileMapper;

    @Autowired
    private CompanyProfileMapper companyProfileMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private UserSupportService userSupportService;

    @Test
    void updateJobseekerProfile_updatesAccountButDoesNotSyncResume() {
        AuthDtos.JobseekerRegisterRequest registerRequest = new AuthDtos.JobseekerRegisterRequest();
        registerRequest.setFullName("资料分离测试");
        registerRequest.setEmail("profile.detached@example.com");
        registerRequest.setPhone("13700002001");
        registerRequest.setPassword("Password123");
        Long userId = ((Number) authService.registerJobseeker(registerRequest).get("userId")).longValue();

        Resume resume = resumeMapper.selectOne(new LambdaQueryWrapper<Resume>().eq(Resume::getUserId, userId));
        assertThat(resume).isNotNull();
        assertThat(resume.getFullName()).isEqualTo("资料分离测试");
        assertThat(resume.getEmail()).isEqualTo("profile.detached@example.com");
        assertThat(resume.getPhone()).isEqualTo("13700002001");

        AuthDtos.JobseekerProfileUpdateRequest updateRequest = new AuthDtos.JobseekerProfileUpdateRequest();
        updateRequest.setFullName("新的求职者名称");
        updateRequest.setEmail("profile.updated@example.com");
        updateRequest.setPhone("13700002002");

        authService.updateJobseekerProfile(userId, updateRequest);

        SysUser user = sysUserMapper.selectById(userId);
        JobseekerProfile profile = jobseekerProfileMapper.selectOne(
                new LambdaQueryWrapper<JobseekerProfile>().eq(JobseekerProfile::getUserId, userId));
        Resume unchangedResume = resumeMapper.selectOne(new LambdaQueryWrapper<Resume>().eq(Resume::getUserId, userId));

        assertThat(user.getDisplayName()).isEqualTo("新的求职者名称");
        assertThat(user.getUsername()).isEqualTo("profile.updated@example.com");
        assertThat(user.getEmail()).isEqualTo("profile.updated@example.com");
        assertThat(user.getPhone()).isEqualTo("13700002002");

        assertThat(profile.getFullName()).isEqualTo("新的求职者名称");
        assertThat(profile.getEmail()).isEqualTo("profile.updated@example.com");
        assertThat(profile.getPhone()).isEqualTo("13700002002");
        assertThat(profile.getPreferredCity()).isNull();
        assertThat(profile.getDesiredPositionCategory()).isNull();

        assertThat(unchangedResume.getFullName()).isEqualTo("资料分离测试");
        assertThat(unchangedResume.getEmail()).isEqualTo("profile.detached@example.com");
        assertThat(unchangedResume.getPhone()).isEqualTo("13700002001");
    }

    @Test
    void updateCompanyProfile_resetsAuditStatusAndCreatesAuditNotificationWhenCoreFieldsChange() {
        AuthDtos.CompanyRegisterRequest registerRequest = new AuthDtos.CompanyRegisterRequest();
        registerRequest.setCompanyName("原企业名称");
        registerRequest.setUnifiedSocialCreditCode("91310000MA1K880001");
        registerRequest.setContactPerson("张经理");
        registerRequest.setPhone("13800002001");
        registerRequest.setEmail("company.profile@example.com");
        registerRequest.setPassword("Password123");
        Long userId = ((Number) authService.registerCompany(registerRequest).get("userId")).longValue();

        CompanyProfile originalProfile = companyProfileMapper.selectOne(
                new LambdaQueryWrapper<CompanyProfile>().eq(CompanyProfile::getUserId, userId));
        originalProfile.setAuditStatus(CompanyAuditStatus.APPROVED.name());
        companyProfileMapper.updateById(originalProfile);

        long notificationCountBefore = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getType, NotificationType.COMPANY_AUDIT.name()));

        AuthDtos.CompanyProfileUpdateRequest updateRequest = new AuthDtos.CompanyProfileUpdateRequest();
        updateRequest.setCompanyName("新企业名称");
        updateRequest.setUnifiedSocialCreditCode("91310000MA1K880002");
        updateRequest.setContactPerson("李经理");
        updateRequest.setPhone("13800002002");
        updateRequest.setEmail("company.profile.updated@example.com");
        updateRequest.setAddress("上海市浦东新区世纪大道 88 号");
        updateRequest.setDescription("企业资料更新后等待重新审核");

        authService.updateCompanyProfile(userId, updateRequest);

        CompanyProfile updatedProfile = companyProfileMapper.selectOne(
                new LambdaQueryWrapper<CompanyProfile>().eq(CompanyProfile::getUserId, userId));
        SysUser updatedUser = sysUserMapper.selectById(userId);
        long notificationCountAfter = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getType, NotificationType.COMPANY_AUDIT.name()));

        assertThat(updatedProfile.getCompanyName()).isEqualTo("新企业名称");
        assertThat(updatedProfile.getUnifiedSocialCreditCode()).isEqualTo("91310000MA1K880002");
        assertThat(updatedProfile.getAuditStatus()).isEqualTo(CompanyAuditStatus.PENDING.name());
        assertThat(updatedUser.getDisplayName()).isEqualTo("新企业名称");
        assertThat(updatedUser.getUsername()).isEqualTo("company.profile.updated@example.com");
        assertThat(notificationCountAfter).isGreaterThan(notificationCountBefore);
    }

    @Test
    void changePassword_rejectsWrongCurrentPasswordAndPersistsNewPassword() {
        AuthDtos.JobseekerRegisterRequest registerRequest = new AuthDtos.JobseekerRegisterRequest();
        registerRequest.setFullName("密码修改测试");
        registerRequest.setEmail("password.update@example.com");
        registerRequest.setPhone("13700002003");
        registerRequest.setPassword("Password123");
        Long userId = ((Number) authService.registerJobseeker(registerRequest).get("userId")).longValue();

        AuthDtos.ChangePasswordRequest wrongPasswordRequest = new AuthDtos.ChangePasswordRequest();
        wrongPasswordRequest.setCurrentPassword("WrongPassword123");
        wrongPasswordRequest.setNewPassword("NewPassword123");
        wrongPasswordRequest.setConfirmPassword("NewPassword123");

        assertThatThrownBy(() -> authService.changePassword(userId, wrongPasswordRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("当前密码不正确");

        AuthDtos.ChangePasswordRequest validRequest = new AuthDtos.ChangePasswordRequest();
        validRequest.setCurrentPassword("Password123");
        validRequest.setNewPassword("NewPassword123");
        validRequest.setConfirmPassword("NewPassword123");
        authService.changePassword(userId, validRequest);

        SysUser updatedUser = sysUserMapper.selectById(userId);
        assertThat(userSupportService.passwordEncoder().matches("NewPassword123", updatedUser.getPassword())).isTrue();
        assertThat(userSupportService.passwordEncoder().matches("Password123", updatedUser.getPassword())).isFalse();
    }
}
