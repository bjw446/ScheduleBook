package com.example.schedulebook.domain.comment.repository;

import com.example.schedulebook.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query("SELECT c from Comment c JOIN FETCH c.writer WHERE c.schedule.id = :scheduleId " +
            "AND c.parent IS NULL ORDER BY c.createdAt ASC")
    List<Comment> findParentComments(@Param("scheduleId") Long scheduleId);

    @Query("SELECT c FROM Comment c JOIN FETCH c.writer WHERE c.parent.id IN :parentIds ORDER BY c.createdAt ASC")
    List<Comment> findReplies(@Param("parentIds") List<Long> parentIds);

    @Query("SELECT c FROM Comment c JOIN FETCH c.writer WHERE c.id = :commentId")
    Optional<Comment> findWithWriter(@Param("commentId") Long commentId);

    long countBySchedule_IdAndDeletedFalse(Long scheduleId);

    boolean existsByIdAndDeletedFalse(Long commentId);

    @Query("SELECT c FROM Comment c WHERE c.writer.id = :userId AND c.deletedAt IS NULL")
    List<Comment> findAllByWriterId(@Param("userId") Long userId);
}
