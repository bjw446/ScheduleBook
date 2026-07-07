package com.example.schedulebook.domain.chat_room.controller;

import com.example.schedulebook.common.enums.SuccessEnum;
import com.example.schedulebook.common.response.ApiResponse;
import com.example.schedulebook.common.security.SecurityUtils;
import com.example.schedulebook.domain.chat_room.dto.request.ChatRoomInviteRequest;
import com.example.schedulebook.domain.chat_room.dto.request.GroupChatRoomCreateRequest;
import com.example.schedulebook.domain.chat_room.dto.request.ChatRoomUpdateNameRequest;
import com.example.schedulebook.domain.chat_room.dto.response.ChatRoomDetailResponse;
import com.example.schedulebook.domain.chat_room.dto.response.ChatRoomListResponse;
import com.example.schedulebook.domain.chat_room.dto.response.ChatRoomResponse;
import com.example.schedulebook.domain.chat_room.service.ChatRoomService;
import jakarta.validation.Valid;
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

    @PostMapping("/group")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> createGroupRoom(
            @Valid @RequestBody GroupChatRoomCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        SuccessEnum.CREATE_SUCCESS,
                        chatRoomService.createGroupRoom(SecurityUtils.getCurrentUserId(), request)
                )
        );
    }

    @PostMapping("/{roomId}/invite")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> inviteMembers(
            @PathVariable Long roomId,
            @Valid @RequestBody ChatRoomInviteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.UPDATE_SUCCESS,
                        chatRoomService.inviteMembers(SecurityUtils.getCurrentUserId(), roomId, request)
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

    @PatchMapping("/{roomId}/name")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> updateRoomName(
            @PathVariable Long roomId,
            @Valid @RequestBody ChatRoomUpdateNameRequest request
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.UPDATE_SUCCESS,
                        chatRoomService.updateRoomName(SecurityUtils.getCurrentUserId(), roomId, request)
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
