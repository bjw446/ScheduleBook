package com.example.schedulebook.domain.auth.dto.properties;

import com.example.schedulebook.domain.user.enums.UserRole;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.session.limit")
public record SessionLimitProperties(
        int user,
        int manager,
        int superAdmin
) {
    public int getLimit(UserRole userRole) {
        return switch (userRole) {
            case USER -> user;
            case MANAGER -> manager;
            case SUPER_ADMIN -> superAdmin;
        };
    }
}
