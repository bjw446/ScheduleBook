package com.example.schedulebook.common.websocket.validator;

public interface SubscriptionValidator {

    // 현재 destination을 이 validator가 처리하는지 여부
    boolean supports(String destination);

    // destination에 대한 권한 검사
    void validate(Long userId, String destination);
}
