package com.bishe.recruitment.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bishe.recruitment.common.BusinessException;
import com.bishe.recruitment.entity.SysRole;
import com.bishe.recruitment.entity.SysUser;
import com.bishe.recruitment.entity.SysUserRole;
import com.bishe.recruitment.enums.UserAccountStatus;
import com.bishe.recruitment.mapper.SysRoleMapper;
import com.bishe.recruitment.mapper.SysUserMapper;
import com.bishe.recruitment.mapper.SysUserRoleMapper;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;

    public CustomUserDetailsService(SysUserMapper sysUserMapper, SysUserRoleMapper sysUserRoleMapper, SysRoleMapper sysRoleMapper) {
        this.sysUserMapper = sysUserMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysRoleMapper = sysRoleMapper;
    }

    public AuthenticatedUser loadByUsername(String username) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        return toAuthenticatedUser(user);
    }

    public AuthenticatedUser loadByAccount(String account) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, account)
                .or().eq(SysUser::getEmail, account)
                .or().eq(SysUser::getPhone, account)
                .last("limit 1"));
        if (user == null) {
            throw new BusinessException(401, "账号或密码错误");
        }
        return toAuthenticatedUser(user);
    }

    public AuthenticatedUser loadByUserId(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        return toAuthenticatedUser(user);
    }

    private AuthenticatedUser toAuthenticatedUser(SysUser user) {
        UserAccountStatus accountStatus = resolveStatus(user.getStatus());
        if (accountStatus == UserAccountStatus.DISABLED) {
            throw new BusinessException(401, "账号已被禁用");
        }
        if (accountStatus == UserAccountStatus.DELETED) {
            throw new BusinessException(401, "账号不存在或已删除");
        }

        List<Long> roleIds = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, user.getId()))
                .stream()
                .map(SysUserRole::getRoleId)
                .toList();
        List<String> roles = roleIds.isEmpty() ? Collections.emptyList() : sysRoleMapper.selectBatchIds(roleIds).stream()
                .map(SysRole::getCode)
                .toList();
        return new AuthenticatedUser(user.getId(), user.getUsername(), user.getPassword(), user.getDisplayName(), roles);
    }

    private UserAccountStatus resolveStatus(String status) {
        try {
            return UserAccountStatus.fromValue(status);
        } catch (IllegalArgumentException ex) {
            return UserAccountStatus.ACTIVE;
        }
    }
}
