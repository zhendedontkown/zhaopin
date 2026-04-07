package com.bishe.recruitment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bishe.recruitment.common.BusinessException;
import com.bishe.recruitment.common.PageResponse;
import com.bishe.recruitment.entity.CompanyProfile;
import com.bishe.recruitment.entity.JobApplication;
import com.bishe.recruitment.entity.JobPost;
import com.bishe.recruitment.entity.SysUser;
import com.bishe.recruitment.enums.CompanyAuditStatus;
import com.bishe.recruitment.enums.NotificationType;
import com.bishe.recruitment.mapper.CompanyProfileMapper;
import com.bishe.recruitment.mapper.JobApplicationMapper;
import com.bishe.recruitment.mapper.JobPostMapper;
import com.bishe.recruitment.mapper.SysUserMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final CompanyProfileMapper companyProfileMapper;
    private final SysUserMapper sysUserMapper;
    private final JobPostMapper jobPostMapper;
    private final JobApplicationMapper jobApplicationMapper;
    private final NotificationService notificationService;

    public AdminService(CompanyProfileMapper companyProfileMapper, SysUserMapper sysUserMapper, JobPostMapper jobPostMapper,
                        JobApplicationMapper jobApplicationMapper, NotificationService notificationService) {
        this.companyProfileMapper = companyProfileMapper;
        this.sysUserMapper = sysUserMapper;
        this.jobPostMapper = jobPostMapper;
        this.jobApplicationMapper = jobApplicationMapper;
        this.notificationService = notificationService;
    }

    public PageResponse<Map<String, Object>> listCompanies(String auditStatus, long pageNum, long pageSize) {
        Page<CompanyProfile> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CompanyProfile> wrapper = new LambdaQueryWrapper<CompanyProfile>().orderByDesc(CompanyProfile::getCreatedAt);
        if (auditStatus != null && !auditStatus.isBlank()) {
            wrapper.eq(CompanyProfile::getAuditStatus, auditStatus);
        }
        Page<CompanyProfile> result = companyProfileMapper.selectPage(page, wrapper);
        List<Map<String, Object>> records = result.getRecords().stream().map(this::toCompanyView).toList();
        return PageResponse.<Map<String, Object>>builder()
                .pageNum(result.getCurrent())
                .pageSize(result.getSize())
                .total(result.getTotal())
                .records(records)
                .build();
    }

    @Transactional
    public Map<String, Object> auditCompany(Long companyUserId, String auditStatus) {
        CompanyAuditStatus status;
        try {
            status = CompanyAuditStatus.valueOf(auditStatus);
        } catch (Exception ex) {
            throw new BusinessException("不支持的审核状态");
        }
        CompanyProfile profile = companyProfileMapper.selectOne(new LambdaQueryWrapper<CompanyProfile>()
                .eq(CompanyProfile::getUserId, companyUserId));
        if (profile == null) {
            throw new BusinessException("企业不存在");
        }
        profile.setAuditStatus(status.name());
        companyProfileMapper.updateById(profile);
        notificationService.createAndPush(companyUserId, NotificationType.COMPANY_AUDIT,
                "企业认证状态已更新", "您的企业认证状态已更新为 " + status.name());
        return toCompanyView(profile);
    }

    public Map<String, Object> dashboard() {
        long companyCount = companyProfileMapper.selectCount(null);
        long approvedCompanyCount = companyProfileMapper.selectCount(new LambdaQueryWrapper<CompanyProfile>()
                .eq(CompanyProfile::getAuditStatus, CompanyAuditStatus.APPROVED.name()));
        long jobCount = jobPostMapper.selectCount(null);
        long publishedJobCount = jobPostMapper.selectCount(new LambdaQueryWrapper<JobPost>().eq(JobPost::getStatus, "PUBLISHED"));
        long applicationCount = jobApplicationMapper.selectCount(null);
        return Map.of(
                "companyCount", companyCount,
                "approvedCompanyCount", approvedCompanyCount,
                "jobCount", jobCount,
                "publishedJobCount", publishedJobCount,
                "applicationCount", applicationCount,
                "activeUsers", sysUserMapper.selectCount(null)
        );
    }

    private Map<String, Object> toCompanyView(CompanyProfile profile) {
        SysUser user = sysUserMapper.selectById(profile.getUserId());
        LinkedHashMap<String, Object> view = new LinkedHashMap<>();
        view.put("userId", profile.getUserId());
        view.put("companyName", profile.getCompanyName());
        view.put("contactPerson", profile.getContactPerson());
        view.put("phone", profile.getPhone());
        view.put("email", profile.getEmail());
        view.put("unifiedSocialCreditCode", profile.getUnifiedSocialCreditCode());
        view.put("auditStatus", profile.getAuditStatus());
        view.put("displayName", user == null ? "" : user.getDisplayName());
        view.put("createdAt", profile.getCreatedAt());
        return view;
    }
}
