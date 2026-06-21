package com.example.schedulebook.domain.scheduleshare.controller;

import com.example.schedulebook.common.enums.SuccessEnum;
import com.example.schedulebook.common.response.ApiResponse;
import com.example.schedulebook.common.security.SecurityUtils;
import com.example.schedulebook.domain.scheduleshare.dto.request.ScheduleShareRequest;
import com.example.schedulebook.domain.scheduleshare.dto.response.*;
import com.example.schedulebook.domain.scheduleshare.service.ScheduleShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/schedule_shares")
public class ScheduleShareController {
    private final ScheduleShareService scheduleShareService;

    @PostMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<ScheduleShareResponse>> shareSchedule(@PathVariable Long scheduleId, @Valid @RequestBody ScheduleShareRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        SuccessEnum.CREATE_SUCCESS, scheduleShareService.shareSchedule(scheduleId, request, SecurityUtils.getCurrentUserId())
                )
        );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<SharedScheduleResponse>>> getAllSharedSchedules() {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS, scheduleShareService.findAllSharedSchedules(SecurityUtils.getCurrentUserId())
                )
        );
    }

    @GetMapping("/{shareId}")
    public ResponseEntity<ApiResponse<SharedScheduleDetailResponse>> getOneSharedSchedule(@PathVariable Long shareId) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS, scheduleShareService.findOneSharedSchedule(shareId, SecurityUtils.getCurrentUserId())
                )
        );
    }

    @GetMapping("/owned")
    public ResponseEntity<ApiResponse<List<OwnedShareResponse>>> getAllOwnedShares() {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS, scheduleShareService.findAllOwnedShares(SecurityUtils.getCurrentUserId())
                )
        );
    }

    @GetMapping("/owned/{shareId}")
    public ResponseEntity<ApiResponse<OwnedShareDetailResponse>> getOneOwnedShareDetail(@PathVariable Long shareId) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS, scheduleShareService.findOneOwnedShareDetail(shareId, SecurityUtils.getCurrentUserId())
                )
        );
    }

    @DeleteMapping("/{shareId}")
    public ResponseEntity<ApiResponse<Void>> cancelShare(@PathVariable Long shareId) {
        scheduleShareService.cancelShare(shareId, SecurityUtils.getCurrentUserId());

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.DELETE_SUCCESS, null
                )
        );
    }
}
