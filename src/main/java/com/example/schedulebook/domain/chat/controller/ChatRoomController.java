package com.example.schedulebook.domain.chat.controller;

import com.example.schedulebook.common.enums.SuccessEnum;
import com.example.schedulebook.common.response.ApiResponse;
import com.example.schedulebook.common.security.SecurityUtils;
import com.example.schedulebook.domain.chat.dto.response.ChatRoomDetailResponse;
import com.example.schedulebook.domain.chat.dto.response.ChatRoomListResponse;
import com.example.schedulebook.domain.chat.dto.response.ChatRoomResponse;
import com.example.schedulebook.domain.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat/rooms")
public class ChatRoomController {
    private final ChatRoomService chatRoomService;

    @PostMapping("/direct/{friendId}")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> createDirectRoom(@PathVariable Long friendId) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS,
                        chatRoomService.createDirectRoom(SecurityUtils.getCurrentUserId(), friendId)
                )
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChatRoomListResponse>>> getMyChatRooms() {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS,
                        chatRoomService.findMyChatRooms(SecurityUtils.getCurrentUserId())
                )
        );
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<ApiResponse<ChatRoomDetailResponse>> getChatRoom(@PathVariable Long roomId) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS,
                        chatRoomService.findChatRoom(SecurityUtils.getCurrentUserId(), roomId)
                )
        );
    }

    @DeleteMapping("/{roomId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveChatRoom(@PathVariable Long roomId) {
        chatRoomService.leaveChatRoom(SecurityUtils.getCurrentUserId(), roomId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.DELETE_SUCCESS,
                        null
                )
        );
    }
}
