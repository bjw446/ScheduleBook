package com.example.schedulebook.domain.user.entity;

import com.example.schedulebook.common.entity.DeleteEntity;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.user.enums.UserRole;
import com.example.schedulebook.domain.user.enums.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends DeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 20, unique = true, name = "login_id")
    private String loginId;

    @NotBlank
    @Column(nullable = false, length = 70)
    private String password;

    @NotBlank
    @Column(nullable = false, length = 50, unique = true)
    private String nickname;

    @NotBlank
    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @NotBlank
    @Column(nullable = false, length = 20, unique = true, name = "phone_number")
    private String phoneNumber;

    @Column(nullable = false)
    private int level;

    @Column(nullable = false)
    private int exp;

    @Column(nullable = false, name = "login_count")
    private int loginCount;

    @Column(nullable = false, name = "schedule_count")
    private int scheduleCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, name = "user_status")
    private UserStatus userStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "user_role")
    private UserRole userRole;

    @Column(nullable = false, name = "login_streak")
    private int loginStreak;

    @Column(name = "last_login_date")
    private LocalDate lastLoginDate;

    @Version
    private Long version;

    public static User create(String loginId, String encodedPassword, String nickname, String email, String phoneNumber) {
        User user = new User();
        user.loginId = loginId;
        user.password = encodedPassword;
        user.nickname = nickname;
        user.email = email;
        user.phoneNumber = phoneNumber;
        user.level = 1;
        user.exp = 0;
        user.loginCount = 0;
        user.scheduleCount = 0;
        user.userStatus = UserStatus.ACTIVE;
        user.userRole = UserRole.USER;
        user.loginStreak = 0;

        return user;
    }

    public void login() {
        ensureActive();
        LocalDate today = LocalDate.now();

        if (today.equals(lastLoginDate)) {
            return;
        }

        if (lastLoginDate != null && lastLoginDate.plusDays(1).equals(today)) {

            loginStreak++;

        } else {

            loginStreak = 1;
        }

        this.loginCount++;

        this.addExp(1);

        this.rewardLoginStreak();

        this.lastLoginDate = today;
    }

    public void increaseScheduleCount() {
        ensureActive();
        this.scheduleCount++;
        this.addExp(3);
    }

    public void addExp(int exp) {
        ensureActive();

        if (exp <= 0) {
            return;
        }

        this.exp += exp;
        levelUp();
    }

    public void withdraw() {
        if (this.userStatus == UserStatus.WITHDRAW) {
            throw new BaseException(ErrorEnum.USER_ALREADY_WITHDRAW);
        }
        this.userStatus = UserStatus.WITHDRAW;
        this.delete();
    }

    private void levelUp() {
        while (exp >= level * 100) {

            this.exp -= level * 100;
            this.level++;
        }
    }

    private void ensureActive() {
        if (this.userStatus != UserStatus.ACTIVE) {
            throw new BaseException(ErrorEnum.USER_NOT_ACTIVE);
        }
    }

    private void rewardLoginStreak() {
        switch (loginStreak) {
            case 7 -> addExp(10);

            case 30 -> addExp(50);

            case 50 -> addExp(70);

            case 100 -> addExp(150);
        }
    }

    public void updateProfile(String nickname, String email, String phoneNumber) {
        ensureActive();

        this.nickname = nickname;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    public void updatePassword(String encodedPassword) {
        ensureActive();

        this.password = encodedPassword;
    }

    public int getRequiredExp() {
        return level * 100;
    }
}

