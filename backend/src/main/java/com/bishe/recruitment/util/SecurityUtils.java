package com.bishe.recruitment.util;

import com.bishe.recruitment.common.BusinessException;
import com.bishe.recruitment.security.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return user;
    }

    public static Long currentUserId() {
        return currentUser().getUserId();
    }
}
