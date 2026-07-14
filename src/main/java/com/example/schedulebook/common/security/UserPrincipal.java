package com.example.schedulebook.common.security;

import com.example.schedulebook.domain.user.enums.UserRole;

import java.security.Principal;

public record UserPrincipal(Long userId, UserRole userRole) implements Principal {

    @Override
    public String toString() {
        return "UserPrincipal[userId=" + userId + ", token=****]";
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
