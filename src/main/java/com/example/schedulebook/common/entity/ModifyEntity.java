package com.example.schedulebook.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
public class ModifyEntity extends CreateEntity{
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}