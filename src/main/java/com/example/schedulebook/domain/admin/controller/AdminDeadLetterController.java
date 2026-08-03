package com.example.schedulebook.domain.admin.controller;

import com.example.schedulebook.common.enums.SuccessEnum;
import com.example.schedulebook.common.response.ApiResponse;
import com.example.schedulebook.common.response.PageResponse;
import com.example.schedulebook.common.security.SecurityUtils;
import com.example.schedulebook.domain.admin.dto.response.DeadLetterDetailResponse;
import com.example.schedulebook.domain.admin.dto.response.DeadLetterSummaryResponse;
import com.example.schedulebook.domain.admin.service.AdminDeadLetterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/dead-letters")
public class AdminDeadLetterController {
    private final AdminDeadLetterService adminDeadLetterService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DeadLetterSummaryResponse>>> findAllDeadLetters(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS,
                        adminDeadLetterService.findAllDeadLetters(SecurityUtils.getCurrentUserId(), pageable)
                )
        );
    }

    @GetMapping("/{deadLetterId}")
    public ResponseEntity<ApiResponse<DeadLetterDetailResponse>> findOneDeadLetter(@PathVariable Long deadLetterId) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS,
                        adminDeadLetterService.findOneDeadLetter(SecurityUtils.getCurrentUserId(), deadLetterId)
                )
        );
    }

    @PatchMapping("/{deadLetterId}/recover")
    public ResponseEntity<ApiResponse<Void>> recoverDeadLetter(@PathVariable Long deadLetterId) {
        adminDeadLetterService.recoverDeadLetter(SecurityUtils.getCurrentUserId(), deadLetterId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.UPDATE_SUCCESS,
                        null
                )
        );
    }
}
