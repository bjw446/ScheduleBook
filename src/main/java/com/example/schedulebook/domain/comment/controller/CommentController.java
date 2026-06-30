package com.example.schedulebook.domain.comment.controller;

import com.example.schedulebook.common.response.ApiResponse;
import com.example.schedulebook.domain.comment.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/comments")
public class CommentController {
    private final CommentService commentService;

    @PostMapping("/schedules/{scheduleId}")
    public ResponseEntity<ApiResponse<>> createScheduleComment(@PathVariable Long scheduleId) {

    }
}
