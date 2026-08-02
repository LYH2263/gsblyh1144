package com.example.reminder.service;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DingTalkSendResult {

    private boolean success;
    private String errmsg;

    public static DingTalkSendResult success() {
        return new DingTalkSendResult(true, null);
    }

    public static DingTalkSendResult fail(String errmsg) {
        return new DingTalkSendResult(false, errmsg);
    }
}
