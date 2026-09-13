package com.codewithpcodes.glimserver.audit;

import com.codewithpcodes.glimserver.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    private static final Set<String> REDACTED = Set.of(
            "passwordHash", "password", "tokenHash", "codeHash",
            "refreshToken", "accessToken", "secret", "encryptedPin",
            "clientSecret", "apiKey"
    );

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID actorId, String action, String entityType,
                       UUID entityId, Object before, Object after
    ) {
        write(actorId, action, entityType, entityId, before, after);
    }

    /* When the actor is the authenticated caller. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String action, String entityType,
                       UUID entityId, Object before, Object after) {
        write(currentActor(), action, entityType, entityId, before, after);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordIndependently(
            UUID actorID,
            String action,
            String entityType,
            UUID entityID,
            Object before,
            Object after
    ) {
        write(actorID, action, entityType, entityID, before, after);
    }

    private void write(UUID actorId, String action, String entityType,
                       UUID entityId, Object before, Object after) {
        try {
            auditLogRepository.save(AuditLog.builder()
                    .actorId(actorId)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .beforeState(toJson(before))
                    .afterState(toJson(after))
                    .ipAddress(currentIp())
                    .build());

        } catch (Exception e) {
            // Never throw. A failure to log must not roll back a legitimate
            // payment. But log loudly — silent audit loss is its own problem.
            log.error("AUDIT WRITE FAILED — action={} entity={}:{} actor={}",
                    action, entityType, entityId, actorId, e);
        }
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            if (value instanceof String s) return objectMapper.writeValueAsString(s);
            return objectMapper.writeValueAsString(redact(value));
        } catch (Exception e) {
            log.warn("Could not serialise audit state", e);
            return "\"<unserialisable>\"";
        }
    }

    @SuppressWarnings("unchecked")
    private Object redact(Object value) {
        Map<String, Object> map;
        try {
            map = objectMapper.convertValue(value, Map.class);
        } catch (Exception e) {
            return value;
        }

        Map<String, Object> cleaned = new LinkedHashMap<>();
        map.forEach((key, v) ->
                cleaned.put(key, REDACTED.contains(key) ? "***" : v));
        return cleaned;
    }

    private UUID currentActor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        if (auth.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return null;
    }

    private String currentIp() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servlet)) return null;

        HttpServletRequest request = servlet.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null && !forwarded.isBlank()
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }
}
