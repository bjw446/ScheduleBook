package com.example.schedulebook.domain.admin.controller;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.enums.SuccessEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.response.ApiResponse;
import com.example.schedulebook.common.response.PageResponse;
import com.example.schedulebook.common.security.SecurityUtils;
import com.example.schedulebook.domain.admin.dto.response.CleanupOutboxResponse;
import com.example.schedulebook.domain.admin.dto.response.DeadOutboxResponse;
import com.example.schedulebook.domain.admin.dto.response.OutboxStatsResponse;
import com.example.schedulebook.domain.admin.service.AdminOutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/outboxes")
public class AdminOutboxController {
    private final AdminOutboxService adminOutboxService;

    @GetMapping("/dead")
    public ResponseEntity<ApiResponse<PageResponse<DeadOutboxResponse>>> findAllDeadOutboxes(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS,
                        adminOutboxService.findAllDeadOutboxes(SecurityUtils.getCurrentUserId(), pageable)
                )
        );
    }

    @PatchMapping("/{outboxId}/retry")
    public ResponseEntity<ApiResponse<Void>> retryDeadOutbox(@PathVariable Long outboxId) {
        adminOutboxService.retryDeadOutbox(SecurityUtils.getCurrentUserId(), outboxId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.UPDATE_SUCCESS,
                        null
                )
        );
    }

    @DeleteMapping("/success")
    public ResponseEntity<ApiResponse<CleanupOutboxResponse>> deleteSuccessOutbox(@RequestParam(defaultValue = "30") int days) {
        if (days < 1) {
            throw new BaseException(ErrorEnum.INVALID_INPUT);
        }

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.DELETE_SUCCESS,
                        adminOutboxService.deleteSuccessOutbox(SecurityUtils.getCurrentUserId(), days)
                )
        );
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<OutboxStatsResponse>> getStats() {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS,
                        adminOutboxService.getStats(SecurityUtils.getCurrentUserId())
                )
        );
    }
}
