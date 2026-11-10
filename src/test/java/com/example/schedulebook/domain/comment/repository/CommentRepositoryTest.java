package com.example.schedulebook.domain.comment.repository;

import com.example.schedulebook.common.config.JpaAuditingConfig;
import com.example.schedulebook.domain.comment.entity.Comment;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    // ========================================
    // findParentComments
    // ========================================

    @Test
    @DisplayName("특정 일정의 부모 댓글을 생성 시간 오름차순으로 조회한다")
    void findParentComments() throws InterruptedException {
        // given
        User writer = createUser("1");
        User anotherWriter = createUser("2");

        Schedule schedule = createSchedule(writer, "1");
        Schedule anotherSchedule = createSchedule(anotherWriter, "2");

        persist(writer, anotherWriter, schedule, anotherSchedule);

        Comment firstComment = Comment.create(schedule, writer, "첫 번째 부모 댓글");
        persist(firstComment);
        entityManager.flush();

        Thread.sleep(20);

        Comment secondComment = Comment.create(schedule, anotherWriter, "두 번째 부모 댓글");
        persist(secondComment);
        entityManager.flush();

        Thread.sleep(20);

        Comment anotherScheduleComment = Comment.create(
                anotherSchedule, anotherWriter, "다른 일정의 부모 댓글"
        );
        persist(anotherScheduleComment);

        flushAndClear();

        // when
        List<Comment> result = commentRepository.findParentComments(schedule.getId());

        // then
        assertThat(result)
                .hasSize(2)
                .extracting(Comment::getId)
                .containsExactly(firstComment.getId(), secondComment.getId());

        assertThat(result)
                .extracting(Comment::getParent)
                .containsOnlyNulls();

        assertThat(result.get(0).getCreatedAt())
                .isBefore(result.get(1).getCreatedAt());
    }

    @Test
    @DisplayName("부모 댓글 조회 시 대댓글은 제외한다")
    void findParentCommentsExcludingReplies() {
        // given
        User writer = createUser("1");
        User replyWriter = createUser("2");

        Schedule schedule = createSchedule(writer, "1");

        persist(writer, replyWriter, schedule);

        Comment parent = Comment.create(schedule, writer, "부모 댓글");
        persist(parent);

        Comment reply = Comment.reply(schedule, replyWriter, parent, "대댓글");
        persist(reply);

        // when
        List<Comment> result = commentRepository.findParentComments(schedule.getId());

        // then
        assertThat(result)
                .hasSize(1)
                .extracting(Comment::getId)
                .containsExactly(parent.getId());
    }

    @Test
    @DisplayName("부모 댓글 조회 시 작성자는 Fetch Join으로 로딩된다")
    void findParentCommentsWithWriter() {
        // given
        User writer = createUser("1");
        Schedule schedule = createSchedule(writer, "1");

        persist(writer, schedule);

        Comment comment = Comment.create(schedule, writer, "부모 댓글");
        persist(comment);

        flushAndClear();

        PersistenceUnitUtil persistenceUnitUtil =
                entityManagerFactory.getPersistenceUnitUtil();

        // when
        List<Comment> result = commentRepository.findParentComments(schedule.getId());

        // then
        assertThat(result).hasSize(1);

        Comment foundComment = result.get(0);

        assertThat(persistenceUnitUtil.isLoaded(foundComment, "writer")).isTrue();
        assertThat(foundComment.getWriter().getId()).isEqualTo(writer.getId());
    }

    // ========================================
    // findReplies
    // ========================================

    @Test
    @DisplayName("지정한 부모 댓글들의 대댓글을 생성 시간 오름차순으로 조회한다")
    void findReplies() throws InterruptedException {
        // given
        User writer = createUser("1");
        User replyWriter = createUser("2");

        Schedule schedule = createSchedule(writer, "1");

        persist(writer, replyWriter, schedule);

        Comment firstParent = Comment.create(schedule, writer, "첫 번째 부모 댓글");
        Comment secondParent = Comment.create(schedule, writer, "두 번째 부모 댓글");
        Comment anotherParent = Comment.create(schedule, writer, "다른 부모 댓글");

        persist(firstParent, secondParent, anotherParent);

        Comment firstReply = Comment.reply(
                schedule, replyWriter, firstParent, "첫 번째 대댓글"
        );
        persist(firstReply);
        entityManager.flush();

        Thread.sleep(20);

        Comment secondReply = Comment.reply(
                schedule, replyWriter, secondParent, "두 번째 대댓글"
        );
        persist(secondReply);
        entityManager.flush();

        Thread.sleep(20);

        Comment anotherReply = Comment.reply(
                schedule, replyWriter, anotherParent, "다른 대댓글"
        );
        persist(anotherReply);

        flushAndClear();

        // when
        List<Comment> result = commentRepository.findReplies(
                List.of(firstParent.getId(), secondParent.getId())
        );

        // then
        assertThat(result)
                .hasSize(2)
                .extracting(Comment::getId)
                .containsExactly(firstReply.getId(), secondReply.getId());

        assertThat(result.get(0).getCreatedAt())
                .isBefore(result.get(1).getCreatedAt());
    }

    @Test
    @DisplayName("대댓글 조회 시 작성자는 Fetch Join으로 로딩된다")
    void findRepliesWithWriter() {
        // given
        User writer = createUser("1");
        User replyWriter = createUser("2");

        Schedule schedule = createSchedule(writer, "1");

        persist(writer, replyWriter, schedule);

        Comment parent = Comment.create(schedule, writer, "부모 댓글");
        persist(parent);

        Comment reply = Comment.reply(schedule, replyWriter, parent, "대댓글");
        persist(reply);

        flushAndClear();

        PersistenceUnitUtil persistenceUnitUtil =
                entityManagerFactory.getPersistenceUnitUtil();

        // when
        List<Comment> result = commentRepository.findReplies(List.of(parent.getId()));

        // then
        assertThat(result).hasSize(1);

        Comment foundReply = result.get(0);

        assertThat(persistenceUnitUtil.isLoaded(foundReply, "writer")).isTrue();
        assertThat(foundReply.getWriter().getId()).isEqualTo(replyWriter.getId());
    }

    // ========================================
    // findWithWriter
    // ========================================

    @Test
    @DisplayName("댓글 ID로 댓글과 작성자를 함께 조회한다")
    void findWithWriter() {
        // given
        User writer = createUser("1");
        Schedule schedule = createSchedule(writer, "1");

        persist(writer, schedule);

        Comment comment = Comment.create(schedule, writer, "댓글 내용");
        persist(comment);

        flushAndClear();

        PersistenceUnitUtil persistenceUnitUtil =
                entityManagerFactory.getPersistenceUnitUtil();

        // when
        Comment foundComment = commentRepository.findWithWriter(comment.getId())
                .orElseThrow();

        // then
        assertThat(foundComment.getId()).isEqualTo(comment.getId());
        assertThat(persistenceUnitUtil.isLoaded(foundComment, "writer")).isTrue();
        assertThat(foundComment.getWriter().getId()).isEqualTo(writer.getId());
    }

    @Test
    @DisplayName("존재하지 않는 댓글 ID로 조회하면 빈 Optional을 반환한다")
    void findWithWriterWhenCommentNotFound() {
        // given
        Long notFoundCommentId = 999L;

        // when
        var result = commentRepository.findWithWriter(notFoundCommentId);

        // then
        assertThat(result).isEmpty();
    }

    // ========================================
    // countBySchedule_IdAndDeletedFalse
    // ========================================

    @Test
    @DisplayName("특정 일정의 삭제되지 않은 댓글 수를 조회한다")
    void countBySchedule_IdAndDeletedFalse() {
        // given
        User writer = createUser("1");
        Schedule schedule = createSchedule(writer, "1");

        persist(writer, schedule);

        Comment firstComment = Comment.create(schedule, writer, "첫 번째 댓글");
        Comment secondComment = Comment.create(schedule, writer, "두 번째 댓글");
        Comment deletedComment = Comment.create(schedule, writer, "삭제할 댓글");

        persist(firstComment, secondComment, deletedComment);

        deletedComment.deleteComment();

        flushAndClear();

        // when
        long result = commentRepository.countBySchedule_IdAndDeletedFalse(schedule.getId());

        // then
        assertThat(result).isEqualTo(2);
    }

    @Test
    @DisplayName("댓글이 없는 일정의 댓글 수는 0이다")
    void countBySchedule_IdAndDeletedFalseWhenNoComments() {
        // given
        User writer = createUser("1");
        Schedule schedule = createSchedule(writer, "1");

        persist(writer, schedule);

        // when
        long result = commentRepository.countBySchedule_IdAndDeletedFalse(schedule.getId());

        // then
        assertThat(result).isZero();
    }

    // ========================================
    // existsByIdAndDeletedFalse
    // ========================================

    @Test
    @DisplayName("삭제되지 않은 댓글은 존재 여부가 true이다")
    void existsByIdAndDeletedFalse() {
        // given
        User writer = createUser("1");
        Schedule schedule = createSchedule(writer, "1");

        persist(writer, schedule);

        Comment comment = Comment.create(schedule, writer, "댓글 내용");
        persist(comment);

        // when
        boolean result = commentRepository.existsByIdAndDeletedFalse(comment.getId());

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("소프트 삭제된 댓글은 존재 여부가 false이다")
    void existsByIdAndDeletedFalseWhenCommentDeleted() {
        // given
        User writer = createUser("1");
        Schedule schedule = createSchedule(writer, "1");

        persist(writer, schedule);

        Comment comment = Comment.create(schedule, writer, "삭제할 댓글");
        persist(comment);

        comment.deleteComment();

        flushAndClear();

        // when
        boolean result = commentRepository.existsByIdAndDeletedFalse(comment.getId());

        // then
        assertThat(result).isFalse();
    }

    // ========================================
    // findAllByWriterId
    // ========================================

    @Test
    @DisplayName("특정 작성자의 삭제되지 않은 댓글 목록을 조회한다")
    void findAllByWriterId() {
        // given
        User writer = createUser("1");
        User anotherWriter = createUser("2");

        Schedule schedule = createSchedule(writer, "1");

        persist(writer, anotherWriter, schedule);

        Comment firstComment = Comment.create(schedule, writer, "첫 번째 댓글");
        Comment secondComment = Comment.create(schedule, writer, "두 번째 댓글");
        Comment deletedComment = Comment.create(schedule, writer, "삭제된 댓글");
        Comment anotherWriterComment = Comment.create(schedule, anotherWriter, "다른 작성자의 댓글");

        persist(firstComment, secondComment, deletedComment, anotherWriterComment);

        deletedComment.deleteComment();

        flushAndClear();

        // when
        List<Comment> result = commentRepository.findAllByWriterId(writer.getId());

        // then
        assertThat(result)
                .hasSize(2)
                .extracting(Comment::getId)
                .containsExactlyInAnyOrder(firstComment.getId(), secondComment.getId());
    }

    // ========================================
    // Test Helpers
    // ========================================

    private User createUser(String suffix) {
        return User.create(
                "login" + suffix,
                "password",
                "nickname" + suffix,
                "email" + suffix + "@test.com",
                "0101234" + suffix
        );
    }

    private Schedule createSchedule(User user, String suffix) {
        return Schedule.create(
                user,
                "일정 제목 " + suffix,
                "일정 내용 " + suffix,
                LocalDate.of(2026, 12, 1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0)
        );
    }

    private void persist(Object... entities) {
        for (Object entity : entities) {
            entityManager.persist(entity);
        }
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}