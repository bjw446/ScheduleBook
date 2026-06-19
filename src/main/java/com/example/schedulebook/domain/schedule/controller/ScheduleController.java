package com.example.schedulebook.domain.schedule.controller;

import com.example.schedulebook.common.enums.SuccessEnum;
import com.example.schedulebook.common.response.ApiResponse;
import com.example.schedulebook.common.security.SecurityUtils;
import com.example.schedulebook.domain.schedule.dto.request.CreateScheduleRequest;
import com.example.schedulebook.domain.schedule.dto.request.UpdateScheduleRequest;
import com.example.schedulebook.domain.schedule.dto.response.ScheduleDetailResponse;
import com.example.schedulebook.domain.schedule.dto.response.ScheduleSummaryResponse;
import com.example.schedulebook.domain.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/schedules")
public class ScheduleController {
    private final ScheduleService scheduleService;

    @PostMapping
    public ResponseEntity<ApiResponse<ScheduleSummaryResponse>> createSchedule(@Valid @RequestBody CreateScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        SuccessEnum.CREATE_SUCCESS, scheduleService.createSchedule(request, SecurityUtils.getCurrentUserId())
                )
        );
    }

    @GetMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<ScheduleDetailResponse>> getOneSchedule(@PathVariable Long scheduleId) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS, scheduleService.findOneSchedule(scheduleId, SecurityUtils.getCurrentUserId())
                )
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduleSummaryResponse>>> getSchedulesByMonth(@RequestParam int year, @RequestParam int month) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS, scheduleService.findSchedulesByMonth(year, month, SecurityUtils.getCurrentUserId())
                )
        );
    }

    @GetMapping("/date")
    public ResponseEntity<ApiResponse<List<ScheduleSummaryResponse>>> getSchedulesByDate(@RequestParam LocalDate date) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS, scheduleService.findSchedulesByDate(date, SecurityUtils.getCurrentUserId())
                )
        );
    }

    @PutMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<ScheduleSummaryResponse>> updateSchedule(@PathVariable Long scheduleId, @Valid @RequestBody UpdateScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.UPDATE_SUCCESS, scheduleService.updateSchedule(scheduleId, request, SecurityUtils.getCurrentUserId())
                )
        );
    }

    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(@PathVariable Long scheduleId) {
        scheduleService.deleteSchedule(scheduleId, SecurityUtils.getCurrentUserId());

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(SuccessEnum.DELETE_SUCCESS, null)
        );
    }
}
