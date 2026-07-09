package com.example.schedulebook.domain.comment.entity;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.common.entity.DeleteEntity;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@Entity
@Table(
        name = "comments",
        indexes = {
                @Index(name = "idx_comment_schedule", columnList = "schedule_id"),
                @Index(name = "idx_comment_parent", columnList = "parent_comment_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends DeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User writer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parent;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(nullable = false)
    private boolean edited;

    public static Comment create(Schedule schedule, User writer, String content) {
        Comment comment = new Comment();

        comment.schedule = schedule;
        comment.writer = writer;
        comment.content = content;
        comment.edited = false;

        return comment;
    }

    public static Comment reply(Schedule schedule, User writer, Comment parent, String content) {
        Comment comment = new Comment();

        comment.schedule = schedule;
        comment.writer = writer;
        comment.parent = parent;
        comment.content = content;
        comment.edited = false;

        return comment;
    }

    public void updateComment(String content) {
        this.content = content;
        this.edited = true;
    }

    public void deleteComment() {
        delete();
        this.content = CommonConst.DELETED_COMMENT;
    }
}
