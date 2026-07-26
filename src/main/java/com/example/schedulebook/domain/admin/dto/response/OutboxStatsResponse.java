package com.example.schedulebook.domain.admin.dto.response;


public record OutboxStatsResponse(
        long pending,
        long processing,
        long failed,
        long dead,
        long success
) {
}
