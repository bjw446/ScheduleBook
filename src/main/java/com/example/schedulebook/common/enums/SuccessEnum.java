package com.example.schedulebook.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SuccessEnum {

    REGISTER_SUCCESS(201, "회원가입에 성공하였습니다."),
    LOGIN_SUCCESS(200,  "로그인에 성공하였습니다."),
    LOGOUT_SUCCESS(200,  "로그아웃에 성공하였습니다."),
    TOKEN_REFRESHED(200, "액세스 토큰이 재발급되었습니다"),

    CREATE_SUCCESS(201,  "데이터 생성에 성공하였습니다."),
    READ_SUCCESS(200,  "데이터 조회에 성공하였습니다."),
    UPDATE_SUCCESS(200,  "데이터 수정에 성공하였습니다."),
    DELETE_SUCCESS(200,  "데이터 삭제에 성공하였습니다.");

    private final int status;
    private final String message;
}