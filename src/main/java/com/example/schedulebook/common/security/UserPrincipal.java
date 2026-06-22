package com.example.schedulebook.common.security;

import java.security.Principal;

public record UserPrincipal(Long userId) implements Principal {

    @Override
    public String toString() {
        return "UserPrincipal[userId=" + userId + ", token=****]";
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
