package com.example.schedulebook.domain.user.entity;

import com.example.schedulebook.common.consts.CommonConst;
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
import java.util.UUID;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends DeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 50, unique = true, name = "login_id")
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
    @Column(nullable = false, length = 50, unique = true, name = "phone_number")
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

    public void withdraw(String encodedPassword) {
        if (this.userStatus == UserStatus.WITHDRAW) {
            throw new BaseException(ErrorEnum.USER_ALREADY_WITHDRAW);
        }

        this.userStatus = UserStatus.WITHDRAW;
        this.loginId = maskLoginId(this.loginId);
        this.password = encodedPassword;
        this.nickname = maskNickname(this.nickname);
        this.email = maskEmail(this.email);
        this.phoneNumber = maskPhoneNumber(this.phoneNumber);
        this.delete();
    }

    private String maskLoginId(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            return null;
        }

        int prefixLength = Math.min(loginId.length(), 3);

        String prefix = loginId.substring(0, prefixLength);

        int remainLength = Math.max(0, loginId.length() - prefixLength);

        StringBuilder masked = new StringBuilder();

        for (int i = 0; i < remainLength; i++) {
            masked.append("*");
        }

        String uuidPart = UUID.randomUUID().toString().substring(0, 8);

        String result = CommonConst.WITHDRAW_USER + prefix + masked + uuidPart;

        int maxLength = 50;

        if (result.length() > maxLength) {
            result = result.substring(0, maxLength);
        }

        return result;
    }

    private String maskNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return null;
        }

        String prefix = nickname.substring(0, Math.min(2, nickname.length()));
        String random = UUID.randomUUID().toString().substring(0, 8);

        String result = CommonConst.WITHDRAW_USER + prefix + "_****_" + random;

        // DB 컬럼 길이 제한 적용 (예: 50자)
        int maxLength = 50;

        if (result.length() > maxLength) {
            result = result.substring(0, maxLength);
        }

        return result;
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 20);

        String result = random + "@deleted.com";

        // DB 컬럼 길이 제한 적용 (예: 100자)
        int maxLength = 100;

        if (result.length() > maxLength) {
            result = result.substring(0, maxLength);
        }

        return result;
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }

        // 숫자만 추출 (국내/국제 모두)
        String digits = phoneNumber.replaceAll("\\D", "");

        String random = UUID.randomUUID().toString().substring(0, 8);

        // 최소 길이 체크
        if (digits.length() < 7) {
            return String.format("%s%s_%s", CommonConst.WITHDRAW_USER, digits, random);
        }

        // 최소 정보를 위한 뒤 4자리 추출
        String suffix = digits.substring(digits.length() - 4);

        // 국내 번호(010으로 시작, 총 11자리)
        if (digits.startsWith("010") && digits.length() == 11) {
            String prefix = digits.substring(0, 3);

            return String.format("%s%s-****-%s_%s",
                    CommonConst.WITHDRAW_USER, prefix, suffix, random);
        }

        // 국제 번호 → 앞 국가번호(1~3자리)
        int countryCodeLength = Math.min(3, digits.length() - 4);

        String countryCode = digits.substring(0, countryCodeLength);

        return String.format("%s%s-****-%s_%s",
                CommonConst.WITHDRAW_USER, countryCode, suffix, random);
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

    public String getDisplayNickname() {
        if (getDeletedAt() != null) {
            return CommonConst.UNKNOWN_NICKNAME;
        }
        return nickname;
    }
}

