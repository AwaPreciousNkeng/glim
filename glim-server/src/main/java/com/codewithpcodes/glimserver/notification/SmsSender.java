package com.codewithpcodes.glimserver.notification;

public interface SmsSender {
    void send(String toE164, String message);
}
