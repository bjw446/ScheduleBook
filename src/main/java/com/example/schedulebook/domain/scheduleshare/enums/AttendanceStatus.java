package com.example.schedulebook.domain.scheduleshare.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AttendanceStatus {
    PENDING("미정"),
    ACCEPTED("참석"),
    DECLINED("불참");

    private final String description;
}
