package com.example.schedulebook.domain.auth.consts;

public class AuthValidationPatterns {

    private AuthValidationPatterns() {}

    public static final String PASSWORD_COMPLEXITY =
    "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-={}:;<>?,./]).*$";
    public static final String PHONE_NUMBER_COMPLEXITY = "^\\d{2,3}-\\d{3,4}-\\d{4}$";
}
