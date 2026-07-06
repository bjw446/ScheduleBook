package com.example.schedulebook.common.consts;

public final class WebSocketDestination {
    private WebSocketDestination() {}

    public static String chat(){
        return "/topic/chat/";
    }

    public static String chat(Long roomId) {
        return "/topic/chat/" + roomId;
    }

    public static String chatRead(Long roomId) {
        return chat(roomId) + "/read";
    }

    public static String chatDelete(Long roomId) {
        return chat(roomId) + "/delete";
    }

    public static String schedule() {
        return "/topic/schedule/";
    }

    public static String schedule(Long scheduleId) {
        return "/topic/schedule/" + scheduleId;
    }

    public static String scheduleParticipants(Long scheduleId) {
        return schedule(scheduleId) + "/participants";
    }

    public static String scheduleComment(Long scheduleId) {
        return schedule(scheduleId) + "/comments";
    }
}
