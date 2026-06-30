package com.example.schedulebook.domain.comment.controller;

import com.example.schedulebook.common.enums.SuccessEnum;
import com.example.schedulebook.common.response.ApiResponse;
import com.example.schedulebook.common.security.SecurityUtils;
import com.example.schedulebook.domain.comment.dto.request.CreateScheduleCommentRequest;
import com.example.schedulebook.domain.comment.dto.request.UpdateScheduleCommentRequest;
import com.example.schedulebook.domain.comment.dto.response.ScheduleCommentResponse;
import com.example.schedulebook.domain.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/comments")
public class CommentController {
    private final CommentService commentService;

    @PostMapping("/schedules/{scheduleId}")
    public ResponseEntity<ApiResponse<Void>> createScheduleComment(
            @PathVariable Long scheduleId,
            @Valid @RequestBody CreateScheduleCommentRequest request
    ) {
        commentService.createComment(SecurityUtils.getCurrentUserId(), scheduleId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        SuccessEnum.CREATE_SUCCESS,
                        null
                )
        );
    }

    @GetMapping("/schedules/{scheduleId}")
    public ResponseEntity<ApiResponse<List<ScheduleCommentResponse>>> getAllComment(@PathVariable Long scheduleId) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS,
                        commentService.findAllComment(SecurityUtils.getCurrentUserId(), scheduleId)
                )
        );
    }

    @PatchMapping("/schedules/{commentId}")
    public ResponseEntity<ApiResponse<Void>> updateScheduleComment(
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateScheduleCommentRequest request
    ) {
        commentService.updateComment(SecurityUtils.getCurrentUserId(), commentId, request);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.UPDATE_SUCCESS,
                        null
                )
        );
    }

    @DeleteMapping("/schedules/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteScheduleComment(@PathVariable Long commentId) {
        commentService.deleteComment(SecurityUtils.getCurrentUserId(), commentId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.DELETE_SUCCESS,
                        null
                )
        );
    }
}
