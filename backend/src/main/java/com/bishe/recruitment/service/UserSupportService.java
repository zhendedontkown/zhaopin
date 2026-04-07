package com.bishe.recruitment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bishe.recruitment.common.BusinessException;
import com.bishe.recruitment.entity.CompanyProfile;
import com.bishe.recruitment.entity.JobseekerProfile;
import com.bishe.recruitment.entity.SysRole;
import com.bishe.recruitment.entity.SysUser;
import com.bishe.recruitment.entity.SysUserRole;
import com.bishe.recruitment.enums.UserRole;
import com.bishe.recruitment.mapper.CompanyProfileMapper;
import com.bishe.recruitment.mapper.JobseekerProfileMapper;
import com.bishe.recruitment.mapper.SysRoleMapper;
import com.bishe.recruitment.mapper.SysUserMapper;
import com.bishe.recruitment.mapper.SysUserRoleMapper;
import java.util.Collections;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserSupportService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final CompanyProfileMapper companyProfileMapper;
    private final JobseekerProfileMapper jobseekerProfileMapper;
    private final PasswordEncoder passwordEncoder;

    public UserSupportService(SysUserMapper sysUserMapper, SysRoleMapper sysRoleMapper, SysUserRoleMapper sysUserRoleMapper,
                              CompanyProfileMapper companyProfileMapper, JobseekerProfileMapper jobseekerProfileMapper,
                              PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.companyProfileMapper = companyProfileMapper;
        this.jobseekerProfileMapper = jobseekerProfileMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public void ensureAccountUnique(String email, String phone) {
        if (sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email)) > 0) {
            throw new BusinessException("邮箱已被注册");
        }
        if (sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, phone)) > 0) {
            throw new BusinessException("手机号已被注册");
        }
    }

    public SysUser createUser(String email, String phone, String rawPassword, String displayName, UserRole role) {
        ensureAccountUnique(email, phone);
        SysUser user = new SysUser();
        user.setUsername(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEmail(email);
        user.setPhone(phone);
        user.setDisplayName(displayName);
        user.setStatus("ACTIVE");
        sysUserMapper.insert(user);
        assignRole(user.getId(), role);
        return user;
    }

    public void assignRole(Long userId, UserRole role) {
        SysRole sysRole = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, role.name()));
        if (sysRole == null) {
            throw new BusinessException("系统角色未初始化: " + role.name());
        }
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(sysRole.getId());
        sysUserRoleMapper.insert(userRole);
    }

    public SysUser getUserById(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    public CompanyProfile getCompanyProfileByUserId(Long userId) {
        return companyProfileMapper.selectOne(new LambdaQueryWrapper<CompanyProfile>().eq(CompanyProfile::getUserId, userId));
    }

    public JobseekerProfile getJobseekerProfileByUserId(Long userId) {
        return jobseekerProfileMapper.selectOne(new LambdaQueryWrapper<JobseekerProfile>().eq(JobseekerProfile::getUserId, userId));
    }

    public String getDisplayName(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        return user == null ? "" : user.getDisplayName();
    }

    public List<String> getRoles(Long userId) {
        List<Long> roleIds = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .toList();
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return sysRoleMapper.selectBatchIds(roleIds).stream().map(SysRole::getCode).toList();
    }

    public String getPrimaryRole(Long userId) {
        List<String> roles = getRoles(userId);
        return roles.isEmpty() ? "" : roles.getFirst();
    }

    public List<Long> listUserIdsByRole(UserRole role) {
        SysRole sysRole = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, role.name()));
        if (sysRole == null) {
            return List.of();
        }
        return sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, sysRole.getId()))
                .stream()
                .map(SysUserRole::getUserId)
                .toList();
    }

    public PasswordEncoder passwordEncoder() {
        return passwordEncoder;
    }
}
