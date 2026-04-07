package com.bishe.recruitment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

public final class AuthDtos {

    private AuthDtos() {
    }

    @Data
    public static class CompanyRegisterRequest {
        @NotBlank(message = "企业名称不能为空")
        private String companyName;

        @NotBlank(message = "统一社会信用代码不能为空")
        @Size(min = 18, max = 18, message = "统一社会信用代码必须为18位")
        private String unifiedSocialCreditCode;

        @NotBlank(message = "联系人不能为空")
        private String contactPerson;

        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        private String phone;

        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 32, message = "密码长度需在8到32之间")
        private String password;
    }

    @Data
    public static class JobseekerRegisterRequest {
        @NotBlank(message = "姓名不能为空")
        private String fullName;

        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        private String phone;

        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 32, message = "密码长度需在8到32之间")
        private String password;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "账号不能为空")
        private String account;

        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class LoginResponse {
        private Long userId;
        private String token;
        private String role;
        private String displayName;
    }
}
