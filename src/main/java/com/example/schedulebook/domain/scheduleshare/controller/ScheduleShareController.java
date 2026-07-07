package com.example.schedulebook.domain.scheduleshare.controller;

import com.example.schedulebook.common.enums.SuccessEnum;
import com.example.schedulebook.common.response.ApiResponse;
import com.example.schedulebook.common.security.SecurityUtils;
import com.example.schedulebook.domain.scheduleparticipant.dto.response.ScheduleParticipantListResponse;
import com.example.schedulebook.domain.scheduleshare.dto.request.UpdateAttendanceRequest;
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

    // 다른 사람에게 공유 받은 일정 목록 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<SharedScheduleResponse>>> getAllSharedSchedules() {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS, scheduleShareService.findAllSharedSchedules(SecurityUtils.getCurrentUserId())
                )
        );
    }

    // 다른 사람에게 공유 받은 일정 상세 조회
    @GetMapping("/{shareId}")
    public ResponseEntity<ApiResponse<SharedScheduleDetailResponse>> getOneSharedSchedule(@PathVariable Long shareId) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS, scheduleShareService.findOneSharedSchedule(shareId, SecurityUtils.getCurrentUserId())
                )
        );
    }

    // 내가 다른 사람에게 공유한 일정 목록 조회
    @GetMapping("/owned")
    public ResponseEntity<ApiResponse<List<OwnedShareResponse>>> getAllOwnedShares() {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS, scheduleShareService.findAllOwnedShares(SecurityUtils.getCurrentUserId())
                )
        );
    }

    // 내가 다른 사람에게 공유한 일정 상세 조회
    @GetMapping("/owned/{shareId}")
    public ResponseEntity<ApiResponse<OwnedShareDetailResponse>> getOneOwnedShareDetail(@PathVariable Long shareId) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS, scheduleShareService.findOneOwnedShareDetail(shareId, SecurityUtils.getCurrentUserId())
                )
        );
    }

    // 일정 참가자 목록 조회
    @GetMapping("/{scheduleId}/participants")
    public ResponseEntity<ApiResponse<ScheduleParticipantListResponse>> getParticipants(@PathVariable Long scheduleId) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS,
                        scheduleShareService.findParticipants(SecurityUtils.getCurrentUserId(), scheduleId)
                )
        );
    }

    // 일정 참가 여부
    @PatchMapping("/{scheduleId}/attendance")
    public ResponseEntity<ApiResponse<Void>> updateAttendanceStatus(@PathVariable Long scheduleId, @Valid @RequestBody UpdateAttendanceRequest request) {
        scheduleShareService.updateAttendance(SecurityUtils.getCurrentUserId(), scheduleId, request);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.UPDATE_SUCCESS,
                        null
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
