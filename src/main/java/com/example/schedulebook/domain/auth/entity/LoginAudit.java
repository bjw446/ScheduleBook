package com.example.schedulebook.domain.auth.entity;

import com.example.schedulebook.common.entity.CreateEntity;
import com.example.schedulebook.domain.auth.enums.LoginResult;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@Entity
@Table(name = "login_audit")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginAudit extends CreateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String loginId;

    @Enumerated(EnumType.STRING)
    private LoginResult loginResult;

    private String ipAddress;

    private String userAgent;

    public static LoginAudit create(String loginId, LoginResult loginResult, String ipAddress, String userAgent) {
        LoginAudit loginAudit = new LoginAudit();

        loginAudit.loginId = loginId;
        loginAudit.loginResult = loginResult;
        loginAudit.ipAddress = ipAddress;
        loginAudit.userAgent = userAgent;

        return loginAudit;
    }
}
