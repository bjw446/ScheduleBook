package com.example.schedulebook.domain.friend.entity;

import com.example.schedulebook.common.entity.DeleteEntity;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.friend.enums.FriendStatus;
import com.example.schedulebook.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "friends",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "requester_id",
                                "receiver_id"
                        }
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Friend extends DeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "friend_status")
    private FriendStatus friendStatus;

    public static Friend request(User requester, User receiver) {
        Friend friend = new Friend();

        friend.requester = requester;
        friend.receiver = receiver;
        friend.friendStatus = FriendStatus.PENDING;

        return friend;
    }

    public void acceptFriend() {
        if (this.friendStatus != FriendStatus.PENDING) {
            throw new BaseException(ErrorEnum.INVALID_FRIEND_STATUS);
        }

        this.friendStatus = FriendStatus.ACCEPTED;
    }

    public void rejectFriend() {
        if (this.friendStatus != FriendStatus.PENDING) {
            throw new BaseException(ErrorEnum.INVALID_FRIEND_STATUS);
        }
        this.friendStatus = FriendStatus.REJECTED;
    }

    public void block() {
        if (this.friendStatus == FriendStatus.DELETED || this.friendStatus == FriendStatus.REJECTED) {
            throw new BaseException(ErrorEnum.INVALID_FRIEND_STATUS);
        }

        if (this.friendStatus == FriendStatus.BLOCKED) {
            throw new BaseException(ErrorEnum.FRIEND_ALREADY_BLOCKED);
        }
        this.friendStatus = FriendStatus.BLOCKED;
    }

    public void deleteFriend() {
        if (this.friendStatus == FriendStatus.DELETED) {
            throw new BaseException(ErrorEnum.FRIEND_ALREADY_DELETED);
        }

        if (this.friendStatus != FriendStatus.ACCEPTED) {
            throw new BaseException(ErrorEnum.INVALID_FRIEND_STATUS);
        }

        this.friendStatus = FriendStatus.DELETED;
        this.delete();
    }

    public void reRequest() {
        if (this.friendStatus != FriendStatus.REJECTED && this.friendStatus != FriendStatus.DELETED) {
            throw new BaseException(ErrorEnum.INVALID_FRIEND_STATUS);
        }

        this.friendStatus = FriendStatus.PENDING;
        this.restore();
    }
}
