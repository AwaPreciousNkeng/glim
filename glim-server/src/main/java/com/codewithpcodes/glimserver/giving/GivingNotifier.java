package com.codewithpcodes.glimserver.giving;

import com.codewithpcodes.glimserver.giving.transaction.Transaction;
import com.codewithpcodes.glimserver.notification.NotificationDispatcher;
import com.codewithpcodes.glimserver.notification.NotificationService;
import com.codewithpcodes.glimserver.notification.NotificationType;
import com.codewithpcodes.glimserver.user.Role;
import com.codewithpcodes.glimserver.user.User;
import com.codewithpcodes.glimserver.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class GivingNotifier {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public void notifyStateChange(Transaction transaction) {
        NotificationType type = switch (transaction.getState()) {
            case SUCCESS, ADJUSTED -> NotificationType.PAYMENT_SUCCESS;
            case FAILED, EXPIRED   -> NotificationType.PAYMENT_FAILED;
            default -> null;
        };

        if (type == null) return;

        userRepository.findById(transaction.getUserId()).ifPresent(user ->
                notificationService.notifyAsync(
                        new NotificationDispatcher.Recipient(
                                user.getId(), user.getLanguage().name(),
                                user.getEmail()
                        ),
                        type,
                        "/giving/history",
                        transaction.getCategory().name(user.getLanguage().name()),
                        format(transaction.getAmountMinorUnits())
                ));
    }

    public void alertReconciler(Transaction transaction) {
        List<User> admins = userRepository.findByRole(Role.ADMIN);
        admins.forEach(admin -> notificationService.notifyAsync(
                        new NotificationDispatcher.Recipient(
                                admin.getId(), admin.getLanguage().name(), admin.getEmail()),
                                NotificationType.RECONCILIATION_NEEDED, "/admin/reconciliation",
                                transaction.getReference()));
    }

    private String format(long amount) {
        return NumberFormat.getInstance(Locale.FRANCE).format(amount);
    }
}
