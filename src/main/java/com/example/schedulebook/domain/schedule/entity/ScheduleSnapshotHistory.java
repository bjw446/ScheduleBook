package com.example.schedulebook.domain.schedule.entity;

import com.example.schedulebook.domain.chat.entity.ChatMessage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "schedule_snapshot_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleSnapshotHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_message_id", nullable = false)
    private ChatMessage chatMessage;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "title",
                    column = @Column(name = "snapshot_title")
            ),
            @AttributeOverride(
                    name = "content",
                    column = @Column(name = "snapshot_content", length = 1000)
            ),
            @AttributeOverride(
                    name = "scheduleDate",
                    column = @Column(name = "snapshot_schedule_date")
            ),
            @AttributeOverride(
                    name = "startTime",
                    column = @Column(name = "snapshot_start_time")
            ),
            @AttributeOverride(
                    name = "endTime",
                    column = @Column(name = "snapshot_end_time")
            ),
            @AttributeOverride(
                    name = "scheduleVersion",
                    column = @Column(name = "snapshot_version")
            ),
            @AttributeOverride(
                    name = "scheduleUpdatedAt",
                    column = @Column(name = "snapshot_updated_at")
            )
    })
    private ScheduleSnapshot scheduleSnapshot;

    public static ScheduleSnapshotHistory of(ChatMessage chatMessage, ScheduleSnapshot scheduleSnapshot) {
        ScheduleSnapshotHistory scheduleSnapshotHistory = new ScheduleSnapshotHistory();

        scheduleSnapshotHistory.chatMessage = chatMessage;
        scheduleSnapshotHistory.scheduleSnapshot = scheduleSnapshot;

        return scheduleSnapshotHistory;
    }
}
