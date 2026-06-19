package com.example.schedulebook.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "아이디는 필수 입력 사항 입니다.")
        @Size(max = 20, message = "아이디는 최대 20자 까지 입력 가능합니다.")
        String loginId,

        @NotBlank(message = "비밀번호는 필수 입력 사항 입니다.")
        @Size(min = 8, max = 15, message = "비밀번호는 8자 이상 15자 이하까지 가능합니다.")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-={}:;<>?,./]).*$",
                message = "비밀번호는 영문 대소문자, 숫자, 특수문자를 모두 포함해야 합니다."
        )
        String password
) {
}
