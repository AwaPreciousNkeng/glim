package com.codewithpcodes.glimserver.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!dev")
@RequiredArgsConstructor
public class TwilioSmsSender implements SmsSender {


    @Override
    public void send(String toE164, String message) {

    }
}
