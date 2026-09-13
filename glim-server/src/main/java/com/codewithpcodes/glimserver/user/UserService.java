package com.codewithpcodes.glimserver.user;

import com.codewithpcodes.glimserver.audit.AuditAction;
import com.codewithpcodes.glimserver.audit.AuditEntity;
import com.codewithpcodes.glimserver.audit.AuditService;
import com.codewithpcodes.glimserver.auth.AuthenticationService;
import com.codewithpcodes.glimserver.auth.dtos.ChangeRoleRequest;
import com.codewithpcodes.glimserver.exceptions.BadRequestException;
import com.codewithpcodes.glimserver.exceptions.ResourceNotFoundException;
import com.codewithpcodes.glimserver.notification.NotificationService;
import com.codewithpcodes.glimserver.notification.NotificationType;
import com.codewithpcodes.glimserver.notification.Recipient;
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
    private final AuditService auditService;

    @Transactional
    public void changeRole(ChangeRoleRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (user.getRole() == request.newRole()) {
            throw new BadRequestException("User is already assigned to this role.");
        }

        var previousRole = user.getRole();

        user.setRole(request.newRole());
        userRepository.save(user);
        authenticationService.revokeAllUserTokens(user);

        auditService.record(
                AuditAction.ROLE_CHANGED,
                AuditEntity.USER,
                user.getId(),
                previousRole,
                user.getRole()
        );

        notificationService.notifyAsync(
                Recipient.from(user),
                NotificationType.ROLE_GRANTED,
                "/profile",
                request.newRole().name()
        );
    }
}
