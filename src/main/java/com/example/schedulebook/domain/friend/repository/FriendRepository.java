package com.example.schedulebook.domain.friend.repository;

import com.example.schedulebook.domain.friend.entity.Friend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<Friend, Long> {
    @Query("SELECT f FROM Friend f JOIN FETCH f.requester JOIN  FETCH f.receiver WHERE (f.requester.id = :userId OR f.receiver.id = :userId) AND f.friendStatus = 'ACCEPTED'")
    List<Friend> findAcceptedFriends(Long userId);

    @Query("SELECT f FROM Friend f JOIN FETCH f.requester JOIN FETCH f.receiver WHERE f.receiver.id = :userId AND f.friendStatus = 'PENDING' ")
    List<Friend> findReceivedRequests(Long userId);

    @Query("SELECT f FROM Friend f JOIN FETCH f.requester JOIN FETCH f.receiver WHERE f.requester.id = :userId AND f.friendStatus = 'PENDING' ")
    List<Friend> findSentRequests(Long userId);

    @Query("SELECT f FROM Friend f JOIN FETCH f.requester JOIN FETCH f.receiver WHERE f.id = :friendId ")
    Optional<Friend> findByIdWithUsers(Long friendId);

    @Query("SELECT f FROM Friend f WHERE (f.requester.id = :userId1 AND f.receiver.id = :userId2) OR (f.requester.id = :userId2 AND f.receiver.id = :userId1)")
    Optional<Friend> findRelation(Long userId1, Long userId2);

    @Query("SELECT COUNT(f) > 0 FROM Friend f WHERE ((f.requester.id = :userId1 AND f.receiver.id = :userId2) OR (f.requester.id = :userId2 AND f.receiver.id = :userId1)) AND f.friendStatus = 'ACCEPTED' ")
    boolean existsAcceptedFriend(Long userId1, Long userId2);
}
