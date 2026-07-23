package com.example.schedulebook.domain.user.dto.request;

import com.example.schedulebook.domain.auth.consts.AuthValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank(message = "닉네임은 필수 입력 사항 입니다.")
        @Size(max = 10, message = "닉네임은 최대 10자 까지 입력 가능합니다.")
        String nickname,

        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @NotBlank(message = "이메일은 필수 입력 사항 입니다.")
        @Size(max = 30, message = "이메일은 최대 30자 까지 입력 가능합니다.")
        String email,

        @NotBlank(message = "전화번호는 필수 입력 사항 입니다.")
        @Size(max = 20, message = "전화번호는 최대 20자리 까지 입력 가능합니다")
        @Pattern(regexp = AuthValidationPatterns.PHONE_NUMBER_COMPLEXITY, message = "전화번호 형식이 올바르지 않습니다.")
        String phoneNumber
) {
}
