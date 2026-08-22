package com.codewithpcodes.glimserver.auth.staff;

import com.codewithpcodes.glimserver.auth.util.PhoneUtil;
import com.codewithpcodes.glimserver.notification.NotificationService;
import com.codewithpcodes.glimserver.token.TokenRepository;
import com.codewithpcodes.glimserver.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class StaffService {

    private final UserRepository userRepository;
    private final StaffInvitationRepository staffInvitationRepository;
    private final TokenRepository tokenRepository;
    private final NotificationService notificationService;

    @Transactional
    public UUID invite(CreateStaffRequest request, UUID createdBy) {
        String phone = PhoneUtil.normalise(request.phoneNumber(), )
    }
}
