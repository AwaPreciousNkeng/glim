package com.codewithpcodes.glimserver.notification;

public interface EmailSender {
    void send(String to, String subject, String htmlBody);
}
