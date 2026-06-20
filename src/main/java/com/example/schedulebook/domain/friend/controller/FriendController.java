package com.example.schedulebook.domain.friend.controller;

import com.example.schedulebook.common.enums.SuccessEnum;
import com.example.schedulebook.common.response.ApiResponse;
import com.example.schedulebook.common.security.SecurityUtils;
import com.example.schedulebook.domain.friend.dto.request.FriendRequest;
import com.example.schedulebook.domain.friend.dto.response.ReceivedFriendRequestResponse;
import com.example.schedulebook.domain.friend.dto.response.FriendResponse;
import com.example.schedulebook.domain.friend.dto.response.FriendSummaryResponse;
import com.example.schedulebook.domain.friend.dto.response.SentFriendRequestResponse;
import com.example.schedulebook.domain.friend.service.FriendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/friends")
public class FriendController {
    private final FriendService friendService;

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<FriendResponse>> requestFriend(@Valid @RequestBody FriendRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        SuccessEnum.CREATE_SUCCESS, friendService.requestFriend(request, SecurityUtils.getCurrentUserId())
                )
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FriendSummaryResponse>>> getAllFriends() {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS, friendService.findAllFriends(SecurityUtils.getCurrentUserId())
                )
        );
    }

    @GetMapping("/requests/received")
    public ResponseEntity<ApiResponse<List<ReceivedFriendRequestResponse>>> getReceivedRequests() {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS, friendService.findReceivedRequests(SecurityUtils.getCurrentUserId())
                )
        );
    }

    @GetMapping("/requests/sent")
    public ResponseEntity<ApiResponse<List<SentFriendRequestResponse>>> getSentRequests() {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS, friendService.findSentRequests(SecurityUtils.getCurrentUserId())
                )
        );
    }

    @PatchMapping("/{friendId}/accept")
    public ResponseEntity<ApiResponse<FriendResponse>> acceptFriend(@PathVariable Long friendId) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.UPDATE_SUCCESS, friendService.acceptFriend(friendId, SecurityUtils.getCurrentUserId())
                )
        );
    }

    @PatchMapping("/{friendId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectFriend(@PathVariable Long friendId) {
        friendService.rejectFriend(friendId, SecurityUtils.getCurrentUserId());

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.UPDATE_SUCCESS, null
                )
        );
    }

    @PatchMapping("/{friendId}/block")
    public ResponseEntity<ApiResponse<Void>> blockFriend(@PathVariable Long friendId) {
        friendService.blockFriend(friendId, SecurityUtils.getCurrentUserId());

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.UPDATE_SUCCESS, null
                )
        );
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<ApiResponse<Void>> deleteFriend(@PathVariable Long friendId) {
        friendService.deleteFriend(friendId, SecurityUtils.getCurrentUserId());

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.DELETE_SUCCESS, null
                )
        );
    }
}
