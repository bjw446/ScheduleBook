package com.example.schedulebook.domain.user.dto.request;

import com.example.schedulebook.domain.auth.consts.AuthValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserPasswordRequest(
        @NotBlank(message = "비밀번호는 필수 입력 사항 입니다.")
        @Size(min = 8, max = 15, message = "비밀번호는 8자 이상 15자 이하까지 가능합니다.")
        @Pattern(
                regexp = AuthValidationPatterns.PASSWORD_COMPLEXITY,
                message = "비밀번호는 영문 대소문자, 숫자, 특수문자를 모두 포함해야 합니다."
        )
        String currentPassword,

        @NotBlank(message = "비밀번호는 필수 입력 사항 입니다.")
        @Size(min = 8, max = 15, message = "비밀번호는 8자 이상 15자 이하까지 가능합니다.")
        @Pattern(
                regexp = AuthValidationPatterns.PASSWORD_COMPLEXITY,
                message = "비밀번호는 영문 대소문자, 숫자, 특수문자를 모두 포함해야 합니다."
        )
        String newPassword
) {
}
