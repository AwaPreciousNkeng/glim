package com.codewithpcodes.glimserver.user;

import com.codewithpcodes.glimserver.auth.AuthenticationService;
import com.codewithpcodes.glimserver.auth.dtos.ChangeRoleRequest;
import com.codewithpcodes.glimserver.exceptions.BadRequestException;
import com.codewithpcodes.glimserver.exceptions.ResourceNotFoundException;
import com.codewithpcodes.glimserver.notification.NotificationDispatcher;
import com.codewithpcodes.glimserver.notification.NotificationService;
import com.codewithpcodes.glimserver.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuthenticationService authenticationService;

    @Transactional
    public void changeRole(ChangeRoleRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (user.getRole() == request.newRole()) {
            throw new BadRequestException("User is already assigned to this role.");
        }

        user.setRole(request.newRole());

        userRepository.save(user);
        authenticationService.revokeAllUserTokens(user);

        notificationService.notifyAsync(
                new NotificationDispatcher.Recipient(user.getId(), user.getLanguage().name(), user.getEmail()),
                NotificationType.ROLE_GRANTED, "/profile", request.newRole().name()
        );
    }
}
