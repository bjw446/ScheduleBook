package com.example.schedulebook.domain.deadletter.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "dead_letter_queues")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeadLetterQueue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO : DLQ 추가 예정
}
