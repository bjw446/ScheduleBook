package com.example.schedulebook.domain.reward.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "level_rewards")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LevelReward {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int level;

    private int rewardExp;

    private String rewardTitle;
}
