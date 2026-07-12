package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.domain.auth.entity.LoginAudit;
import com.example.schedulebook.domain.auth.enums.LoginResult;
import com.example.schedulebook.domain.auth.repository.LoginAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginAuditService {
    private final LoginAuditRepository loginAuditRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(String loginId, LoginResult loginResult, String ip, String userAgent) {
        LoginAudit loginAudit = LoginAudit.create(loginId, loginResult, ip, userAgent);

        loginAuditRepository.save(loginAudit);
    }
}
