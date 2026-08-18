package com.codewithpcodes.glimserver.auth;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class otpService {

    private final StringRedisTemplate redisTemplate;

    @Value("${glim.twilio.trial-number}")
    private String twilioPhoneNumber;

    private static final long OTP_VALID_DURATION = 5;

    public String generateAndSendOtp(String userPhoneNumber) {
        String otp = generateRandomOtp();

        redisTemplate.opsForValue().set(userPhoneNumber, otp, OTP_VALID_DURATION, TimeUnit.MINUTES);

        try {
            Message message = Message.creator(
                    new PhoneNumber(userPhoneNumber),
                    new PhoneNumber(twilioPhoneNumber),
                    "Your verification OTP is: " + otp + ". It is valid for 5 minutes."
            ).create();
            log.info("OTP sent successfully via Twilio. SID: {}", message.getSid());
        } catch (Exception e) {
            log.error("Error sending OTP", e);
            throw new RuntimeException("Failed to send OTP via SMS");
        }
        return "OTP sent successfully to  " + userPhoneNumber;
    }

    public boolean validateOtp(String userPhoneNumber, String userInputOtp) {
        String storedOtp = redisTemplate.opsForValue().get(userPhoneNumber);

        if (storedOtp != null && storedOtp.equals(userInputOtp)) {
            redisTemplate.delete(userPhoneNumber);
        }
        return true;
    }

    private String generateRandomOtp() {
        return new DecimalFormat("000000").format(new Random().nextInt(999999));
    }
}
