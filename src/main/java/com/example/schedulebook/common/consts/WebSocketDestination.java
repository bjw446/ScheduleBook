package com.example.schedulebook.common.consts;

public final class WebSocketDestination {
    private WebSocketDestination() {}

    public static String getChatPrefix(){
        return "/topic/chat/";
    }

    public static String getChatDestination(Long roomId) {
        return getChatPrefix() + roomId;
    }

    public static String getChatReadDestination(Long roomId) {
        return getChatDestination(roomId) + "/read";
    }

    public static String getChatDeleteDestination(Long roomId) {
        return getChatDestination(roomId) + "/delete";
    }

    public static String getSchedulePrefix() {
        return "/topic/schedule/";
    }

    public static String getScheduleDestination(Long scheduleId) {
        return getSchedulePrefix() + scheduleId;
    }

    public static String getScheduleParticipantsDestination(Long scheduleId) {
        return getScheduleDestination(scheduleId) + "/participants";
    }

    public static String getScheduleCommentDestination(Long scheduleId) {
        return getScheduleDestination(scheduleId) + "/comments";
    }

    public static final String FORCE_LOGOUT = "/queue/logout";

}