package com.example.reminder.dto;

public class DingTalkSendResult {

    private final boolean success;
    private final String errmsg;

    public DingTalkSendResult(boolean success, String errmsg) {
        this.success = success;
        this.errmsg = errmsg;
    }

    public static DingTalkSendResult ok() {
        return new DingTalkSendResult(true, null);
    }

    public static DingTalkSendResult fail(String errmsg) {
        return new DingTalkSendResult(false, errmsg);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrmsg() {
        return errmsg;
    }
}
