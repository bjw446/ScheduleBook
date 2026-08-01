package com.example.schedulebook.domain.auth.dispatcher;

import com.example.schedulebook.domain.auth.event.ForceLogoutSessionEvent;

public interface ForceLogoutDispatcher {
    void dispatch(ForceLogoutSessionEvent event);
}
