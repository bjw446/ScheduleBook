package com.example.schedulebook.domain.chat.controller;

import com.example.schedulebook.common.enums.SuccessEnum;
import com.example.schedulebook.common.response.ApiResponse;
import com.example.schedulebook.common.security.SecurityUtils;
import com.example.schedulebook.domain.chat.dto.response.ChatRoomResponse;
import com.example.schedulebook.domain.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("chat_rooms")
public class ChatRoomController {
    private final ChatRoomService chatRoomService;

    @PostMapping("/direct/{friendId}")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> createDirectRoom(@PathVariable Long friendId) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.CREATE_SUCCESS,
                        chatRoomService.createDirectRoom(SecurityUtils.getCurrentUserId(), friendId)
                )
        );
    }
}
