package com.example.schedulebook.domain.schedulesnapshot.service;

import com.example.schedulebook.domain.schedulesnapshot.dto.response.ScheduleSnapshotDiffResponse;
import com.example.schedulebook.domain.schedulesnapshot.dto.response.ScheduleSnapshotFieldChangeResponse;
import com.example.schedulebook.domain.schedulesnapshot.entity.ScheduleSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleSnapshotComparatorTest {

    @Mock
    private ScheduleSnapshot before;

    @Mock
    private ScheduleSnapshot after;

    private ScheduleSnapshotComparator comparator;

    private static final Long BEFORE_VERSION = 1L;
    private static final Long AFTER_VERSION = 2L;

    private static final String BEFORE_TITLE = "기존 일정";
    private static final String AFTER_TITLE = "변경된 일정";

    private static final String BEFORE_CONTENT = "기존 내용";
    private static final String AFTER_CONTENT = "변경된 내용";

    private static final LocalDate BEFORE_DATE =
            LocalDate.of(2026, 9, 24);

    private static final LocalDate AFTER_DATE =
            LocalDate.of(2026, 9, 25);

    private static final LocalTime BEFORE_START_TIME =
            LocalTime.of(10, 0);

    private static final LocalTime AFTER_START_TIME =
            LocalTime.of(11, 0);

    private static final LocalTime BEFORE_END_TIME =
            LocalTime.of(12, 0);

    private static final LocalTime AFTER_END_TIME =
            LocalTime.of(13, 0);

    @BeforeEach
    void setUp() {
        comparator = new ScheduleSnapshotComparator();
    }

    @Test
    void title이_변경되면_변경_내용을_반환한다() {
        // given
        stubSameFields();
        when(before.getTitle()).thenReturn(BEFORE_TITLE);
        when(after.getTitle()).thenReturn(AFTER_TITLE);

        // when
        ScheduleSnapshotDiffResponse result =
                comparator.compare(before, after);

        // then
        assertThat(result.fromVersion()).isEqualTo(BEFORE_VERSION);
        assertThat(result.toVersion()).isEqualTo(AFTER_VERSION);

        assertThat(result.changes())
                .containsExactly(
                        new ScheduleSnapshotFieldChangeResponse(
                                "title",
                                BEFORE_TITLE,
                                AFTER_TITLE
                        )
                );
    }

    @Test
    void content가_변경되면_변경_내용을_반환한다() {
        // given
        stubSameFields();
        when(before.getContent()).thenReturn(BEFORE_CONTENT);
        when(after.getContent()).thenReturn(AFTER_CONTENT);

        // when
        ScheduleSnapshotDiffResponse result =
                comparator.compare(before, after);

        // then
        assertThat(result.changes())
                .containsExactly(
                        new ScheduleSnapshotFieldChangeResponse(
                                "content",
                                BEFORE_CONTENT,
                                AFTER_CONTENT
                        )
                );
    }

    @Test
    void scheduleDate가_변경되면_변경_내용을_반환한다() {
        // given
        stubSameFields();
        when(before.getScheduleDate()).thenReturn(BEFORE_DATE);
        when(after.getScheduleDate()).thenReturn(AFTER_DATE);

        // when
        ScheduleSnapshotDiffResponse result =
                comparator.compare(before, after);

        // then
        assertThat(result.changes())
                .containsExactly(
                        new ScheduleSnapshotFieldChangeResponse(
                                "scheduleDate",
                                BEFORE_DATE,
                                AFTER_DATE
                        )
                );
    }

    @Test
    void startTime이_변경되면_변경_내용을_반환한다() {
        // given
        stubSameFields();
        when(before.getStartTime()).thenReturn(BEFORE_START_TIME);
        when(after.getStartTime()).thenReturn(AFTER_START_TIME);

        // when
        ScheduleSnapshotDiffResponse result =
                comparator.compare(before, after);

        // then
        assertThat(result.changes())
                .containsExactly(
                        new ScheduleSnapshotFieldChangeResponse(
                                "startTime",
                                BEFORE_START_TIME,
                                AFTER_START_TIME
                        )
                );
    }

    @Test
    void endTime이_변경되면_변경_내용을_반환한다() {
        // given
        stubSameFields();
        when(before.getEndTime()).thenReturn(BEFORE_END_TIME);
        when(after.getEndTime()).thenReturn(AFTER_END_TIME);

        // when
        ScheduleSnapshotDiffResponse result =
                comparator.compare(before, after);

        // then
        assertThat(result.changes())
                .containsExactly(
                        new ScheduleSnapshotFieldChangeResponse(
                                "endTime",
                                BEFORE_END_TIME,
                                AFTER_END_TIME
                        )
                );
    }

    @Test
    void 여러_필드가_변경되면_변경된_필드를_모두_반환한다() {
        // given
        when(before.getTitle()).thenReturn(BEFORE_TITLE);
        when(after.getTitle()).thenReturn(AFTER_TITLE);

        when(before.getContent()).thenReturn(BEFORE_CONTENT);
        when(after.getContent()).thenReturn(AFTER_CONTENT);

        when(before.getScheduleDate()).thenReturn(BEFORE_DATE);
        when(after.getScheduleDate()).thenReturn(AFTER_DATE);

        when(before.getStartTime()).thenReturn(BEFORE_START_TIME);
        when(after.getStartTime()).thenReturn(AFTER_START_TIME);

        when(before.getEndTime()).thenReturn(BEFORE_END_TIME);
        when(after.getEndTime()).thenReturn(AFTER_END_TIME);

        when(before.getScheduleVersion()).thenReturn(BEFORE_VERSION);
        when(after.getScheduleVersion()).thenReturn(AFTER_VERSION);

        // when
        ScheduleSnapshotDiffResponse result =
                comparator.compare(before, after);

        // then
        assertThat(result.fromVersion()).isEqualTo(BEFORE_VERSION);
        assertThat(result.toVersion()).isEqualTo(AFTER_VERSION);

        assertThat(result.changes())
                .containsExactly(
                        new ScheduleSnapshotFieldChangeResponse(
                                "title",
                                BEFORE_TITLE,
                                AFTER_TITLE
                        ),
                        new ScheduleSnapshotFieldChangeResponse(
                                "content",
                                BEFORE_CONTENT,
                                AFTER_CONTENT
                        ),
                        new ScheduleSnapshotFieldChangeResponse(
                                "scheduleDate",
                                BEFORE_DATE,
                                AFTER_DATE
                        ),
                        new ScheduleSnapshotFieldChangeResponse(
                                "startTime",
                                BEFORE_START_TIME,
                                AFTER_START_TIME
                        ),
                        new ScheduleSnapshotFieldChangeResponse(
                                "endTime",
                                BEFORE_END_TIME,
                                AFTER_END_TIME
                        )
                );
    }

    @Test
    void 동일한_snapshot이면_변경_내용이_없다() {
        // given
        stubSameFields();

        // when
        ScheduleSnapshotDiffResponse result =
                comparator.compare(before, after);

        // then
        assertThat(result.fromVersion()).isEqualTo(BEFORE_VERSION);
        assertThat(result.toVersion()).isEqualTo(AFTER_VERSION);
        assertThat(result.changes()).isEmpty();
    }

    @Test
    void null에서_값으로_변경되면_변경으로_판단한다() {
        // given
        stubSameFields();
        when(before.getTitle()).thenReturn(null);
        when(after.getTitle()).thenReturn(AFTER_TITLE);

        // when
        ScheduleSnapshotDiffResponse result =
                comparator.compare(before, after);

        // then
        assertThat(result.changes())
                .containsExactly(
                        new ScheduleSnapshotFieldChangeResponse(
                                "title",
                                null,
                                AFTER_TITLE
                        )
                );
    }

    @Test
    void 값에서_null로_변경되면_변경으로_판단한다() {
        // given
        stubSameFields();
        when(before.getTitle()).thenReturn(BEFORE_TITLE);
        when(after.getTitle()).thenReturn(null);

        // when
        ScheduleSnapshotDiffResponse result =
                comparator.compare(before, after);

        // then
        assertThat(result.changes())
                .containsExactly(
                        new ScheduleSnapshotFieldChangeResponse(
                                "title",
                                BEFORE_TITLE,
                                null
                        )
                );
    }

    @Test
    void null에서_null이면_변경이_없다() {
        // given
        stubSameFields();
        when(before.getTitle()).thenReturn(null);
        when(after.getTitle()).thenReturn(null);

        // when
        ScheduleSnapshotDiffResponse result =
                comparator.compare(before, after);

        // then
        assertThat(result.changes()).isEmpty();
    }

    private void stubSameFields() {
        when(before.getTitle()).thenReturn(BEFORE_TITLE);
        when(after.getTitle()).thenReturn(BEFORE_TITLE);

        when(before.getContent()).thenReturn(BEFORE_CONTENT);
        when(after.getContent()).thenReturn(BEFORE_CONTENT);

        when(before.getScheduleDate()).thenReturn(BEFORE_DATE);
        when(after.getScheduleDate()).thenReturn(BEFORE_DATE);

        when(before.getStartTime()).thenReturn(BEFORE_START_TIME);
        when(after.getStartTime()).thenReturn(BEFORE_START_TIME);

        when(before.getEndTime()).thenReturn(BEFORE_END_TIME);
        when(after.getEndTime()).thenReturn(BEFORE_END_TIME);

        when(before.getScheduleVersion()).thenReturn(BEFORE_VERSION);
        when(after.getScheduleVersion()).thenReturn(AFTER_VERSION);
    }
}