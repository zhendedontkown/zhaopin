package com.bishe.recruitment.enums;

import java.util.Locale;

public enum UserAccountStatus {
    ACTIVE,
    DISABLED,
    DELETED;

    public static UserAccountStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("User status cannot be blank");
        }
        return UserAccountStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
