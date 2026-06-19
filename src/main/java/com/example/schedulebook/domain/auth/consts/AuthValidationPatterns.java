package com.example.schedulebook.domain.auth.consts;

public class AuthValidationPatterns {

    private AuthValidationPatterns() {}

    public static final String PASSWORD_COMPLEXITY =
    "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-={}:;<>?,./]).*$";
}
