package com.example.schedulebook.domain.friend.repository;

import com.example.schedulebook.domain.friend.entity.Friend;
import com.example.schedulebook.domain.friend.enums.FriendStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<Friend, Long> {
    @Query("SELECT f FROM Friend f JOIN FETCH f.requester JOIN  FETCH f.receiver WHERE (f.requester.id = :userId OR f.receiver.id = :userId) AND f.friendStatus = :status ")
    List<Friend> findAcceptedFriends(@Param("userId") Long userId, @Param("status") FriendStatus status);

    @Query("SELECT f FROM Friend f JOIN FETCH f.requester JOIN FETCH f.receiver WHERE f.receiver.id = :userId AND f.friendStatus = :status ")
    List<Friend> findReceivedRequests(@Param("userId") Long userId, @Param("status") FriendStatus status);

    @Query("SELECT f FROM Friend f JOIN FETCH f.requester JOIN FETCH f.receiver WHERE f.requester.id = :userId AND f.friendStatus = :status ")
    List<Friend> findSentRequests(@Param("userId") Long userId, @Param("status") FriendStatus status);

    @Query("SELECT f FROM Friend f JOIN FETCH f.requester JOIN FETCH f.receiver WHERE f.id = :friendId ")
    Optional<Friend> findByIdWithUsers(@Param("friendId") Long friendId);

    @Query("SELECT f FROM Friend f WHERE (f.requester.id = :userId1 AND f.receiver.id = :userId2) OR (f.requester.id = :userId2 AND f.receiver.id = :userId1)")
    Optional<Friend> findRelation(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    @Query("SELECT COUNT(f) > 0 FROM Friend f WHERE ((f.requester.id = :userId1 AND f.receiver.id = :userId2) OR (f.requester.id = :userId2 AND f.receiver.id = :userId1)) AND f.friendStatus = :status ")
    boolean existsAcceptedFriend(@Param("userId1") Long userId1, @Param("userId2") Long userId2, @Param("status") FriendStatus status);

    @Query("SELECT COUNT(f) FROM Friend f WHERE ((f.requester.id = :userId AND f.receiver.id IN :friendIds) OR (f.receiver.id = :userId AND f.requester.id IN :friendIds)) AND f.friendStatus = :status")
    long countAcceptedFriends(
            @Param("userId") Long userId,
            @Param("friendIds") List<Long> friendIds,
            @Param("status") FriendStatus status
    );
}
