package com.example.schedulebook.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "아이디는 필수 입력 사항 입니다.")
        @Size(max = 20, message = "아이디는 최대 20자 까지 입력 가능합니다.")
        String loginId,

        @NotBlank(message = "비밀번호는 필수 입력 사항 입니다.")
        @Size(min = 8, max = 15, message = "비밀번호는 8자 이상 15자 이하까지 가능합니다.")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-={}:;<>?,./]).*$",
                message = "비밀번호는 영문 대소문자, 숫자, 특수문자를 모두 포함해야 합니다."
        )
        String password,

        @NotBlank(message = "닉네임은 필수 입력 사항 입니다.")
        @Size(max = 10, message = "닉네임은 최대 10자 까지 입력 가능합니다.")
        String nickname,

        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @NotBlank(message = "이메일은 필수 입력 사항 입니다.")
        @Size(max = 30, message = "이메일은 최대 30자 까지 입력 가능합니다.")
        String email,

        @NotBlank(message = "전화번호는 필수 입력 사항 입니다.")
        @Size(max = 20, message = "전화번호는 최대 20자리 까지 입력 가능합니다")
        @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.")
        String phoneNumber
) {
}
