package com.example.schedulebook.domain.chatmessage.controller;

import com.example.schedulebook.common.enums.SuccessEnum;
import com.example.schedulebook.common.response.ApiResponse;
import com.example.schedulebook.common.security.SecurityUtils;
import com.example.schedulebook.domain.chatmessage.dto.request.ChatMessageScheduleShareRequest;
import com.example.schedulebook.domain.chatmessage.dto.request.ChatMessageSearchRequest;
import com.example.schedulebook.domain.chatmessage.dto.request.ChatMessageSendRequest;
import com.example.schedulebook.domain.chatmessage.dto.request.ChatReadRequest;
import com.example.schedulebook.domain.chatmessage.dto.response.ChatMessageSliceResponse;
import com.example.schedulebook.domain.chatmessage.service.ChatMessageService;
import com.example.schedulebook.domain.schedulesnapshot.dto.response.SchedulePreviewDetailResponse;
import com.example.schedulebook.domain.schedulesnapshot.dto.response.ScheduleSnapshotDiffResponse;
import com.example.schedulebook.domain.schedulesnapshot.dto.response.ScheduleSnapshotHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Controller
@RequiredArgsConstructor
@RequestMapping("/chat/messages")
public class ChatMessageController {
    private final ChatMessageService chatMessageService;

    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessageSendRequest request) {
        chatMessageService.sendMessage(SecurityUtils.getCurrentUserId(), request);
    }

    @PostMapping("/schedule")
    public ResponseEntity<ApiResponse<Void>> shareSchedule(@RequestBody ChatMessageScheduleShareRequest request) {
        chatMessageService.shareSchedule(SecurityUtils.getCurrentUserId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        SuccessEnum.CREATE_SUCCESS,
                        null
                )
        );
    }

    @PostMapping("/{messageId}/shared-schedule/accept")
    public ResponseEntity<ApiResponse<Void>> acceptSharedSchedule(@PathVariable Long messageId) {
        chatMessageService.acceptSharedSchedule(SecurityUtils.getCurrentUserId(), messageId);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        SuccessEnum.CREATE_SUCCESS,
                        null
                )
        );
    }

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<ApiResponse<ChatMessageSliceResponse>> getMessages(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "30") Integer size
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS,
                        chatMessageService.findMessages(
                                SecurityUtils.getCurrentUserId(),
                                roomId,
                                new ChatMessageSearchRequest(cursor, size)
                        )
                )
        );
    }

    @GetMapping("/{messageId}/shared-schedule")
    public ResponseEntity<ApiResponse<SchedulePreviewDetailResponse>> getSharedSchedule(@PathVariable Long messageId) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS,
                        chatMessageService.findSharedSchedule(SecurityUtils.getCurrentUserId(), messageId)
                )
        );
    }

    @GetMapping("/{messageId}/shared-schedule/history")
    public ResponseEntity<ApiResponse<List<ScheduleSnapshotHistoryResponse>>> getScheduleSnapshotHistory(@PathVariable Long messageId) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS,
                        chatMessageService.findScheduleSnapshotHistory(SecurityUtils.getCurrentUserId(), messageId)
                )
        );
    }

    @GetMapping("/{messageId}/shared-schedule/diff")
    public ResponseEntity<ApiResponse<ScheduleSnapshotDiffResponse>> getScheduleSnapshotDiff(
            @PathVariable Long messageId,
            @RequestParam Long from,
            @RequestParam Long to
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS,
                        chatMessageService.findScheduleSnapshotDiff(SecurityUtils.getCurrentUserId(), messageId, from, to)
                )
        );
    }

    @PostMapping("/{roomId}/read")
    public ResponseEntity<ApiResponse<Void>> readMessage(@PathVariable Long roomId, @RequestBody ChatReadRequest request) {
        chatMessageService.readMessage(SecurityUtils.getCurrentUserId(), roomId, request.lastReadMessageId());

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.UPDATE_SUCCESS,
                        null
                )
        );
    }

    @PatchMapping("/{messageId}/schedule/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelScheduleShare(@PathVariable Long messageId) {
        chatMessageService.cancelScheduleShare(SecurityUtils.getCurrentUserId(), messageId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.UPDATE_SUCCESS,
                        null
                )
        );
    }

    @DeleteMapping("/{roomId}/delete/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(@PathVariable Long roomId, @PathVariable Long messageId) {
        chatMessageService.deleteMessage(SecurityUtils.getCurrentUserId(), roomId, messageId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.DELETE_SUCCESS,
                        null
                )
        );
    }
}
