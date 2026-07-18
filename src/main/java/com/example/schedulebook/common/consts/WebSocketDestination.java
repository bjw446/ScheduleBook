package com.example.schedulebook.common.consts;

public final class WebSocketDestination {
    private WebSocketDestination() {}

    public static String CHAT_PREFIX(){
        return "/topic/chat/";
    }

    public static String CHAT(Long roomId) {
        return "/topic/chat/" + roomId;
    }

    public static String CHAT_READ(Long roomId) {
        return CHAT(roomId) + "/read";
    }

    public static String CHAT_DELETE(Long roomId) {
        return CHAT(roomId) + "/delete";
    }

    public static String SCHEDULE_PREFIX() {
        return "/topic/schedule/";
    }

    public static String SCHEDULE(Long scheduleId) {
        return "/topic/schedule/" + scheduleId;
    }

    public static String SCHEDULE_PARTICIPANTS(Long scheduleId) {
        return SCHEDULE(scheduleId) + "/participants";
    }

    public static String SCHEDULE_COMMENT(Long scheduleId) {
        return SCHEDULE(scheduleId) + "/comments";
    }

    public static final String FORCE_LOGOUT = "/queue/logout";

}
