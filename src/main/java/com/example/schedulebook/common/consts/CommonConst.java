package com.example.schedulebook.common.consts;

public final class CommonConst {
    private CommonConst() {}

    public static final String UNKNOWN_NICKNAME = "알 수 없음";
    public static final String DELETED_MESSAGE = "삭제된 메시지입니다.";
    public static final int MAX_PAGE_SIZE = 100;
    public static final int MAX_NAMES = 50;
    public static final String DELETED_COMMENT = "삭제된 댓글입니다.";
    public static final int MAX_LOGIN_FAIL = 5;
    public static final String SESSION_BLOCK = "session-block-";
    public static final String WITHDRAW_USER = "탈퇴한 사용자_";
    public static final int BATCH_SIZE = 100;
    public static final int MAX_RETRY = 5;
    public static final int MAX_BATCHES_PER_RUN = 5;
    public static final long PROCESSING_TIMEOUT_MINUTES = 10;
    public static final long NEXT_RETRY_DELAY = 30;
}
