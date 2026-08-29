package com.example.darks.repair_auto.realtime.session;

import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class RealtimeSessionRegistry {

    public record SessionInfo(
            String sessionId,
            String principalName,
            ActorType actorType,
            Long actorId,
            UserRole userRole,
            Principal principal
    ) {}

    private final Map<String, SessionInfo> sessionById = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionsByActorKey = new ConcurrentHashMap<>();
    private final Set<String> staffSessionIds = ConcurrentHashMap.newKeySet();
    private final Map<UserRole, Set<String>> sessionsByRole = new ConcurrentHashMap<>();

    public void register(String sessionId, Principal principal) {
        if (sessionId == null || principal == null) {
            return;
        }

        Object details = principal;
        if (principal instanceof Authentication authentication) {
            details = authentication.getPrincipal();
        }

        ActorType actorType = null;
        Long actorId = null;
        UserRole role = null;

        if (details instanceof AuthenticatedUser user) {
            actorType = ActorType.STAFF;
            actorId = user.id();
            role = user.role();
        } else if (details instanceof AuthenticatedMobileActor mobile) {
            actorType = mobile.actorType();
            actorId = mobile.actorId();
        }

        if (actorType == null || actorId == null) {
            return;
        }

        SessionInfo info = new SessionInfo(sessionId, principal.getName(), actorType, actorId, role, principal);
        sessionById.put(sessionId, info);

        String actorKey = toActorKey(actorType, actorId);
        sessionsByActorKey.computeIfAbsent(actorKey, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

        if (actorType == ActorType.STAFF) {
            staffSessionIds.add(sessionId);
            if (role != null) {
                sessionsByRole.computeIfAbsent(role, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
            }
        }
    }

    public void unregister(String sessionId) {
        if (sessionId == null) {
            return;
        }
        SessionInfo removed = sessionById.remove(sessionId);
        if (removed != null) {
            String actorKey = toActorKey(removed.actorType(), removed.actorId());
            Set<String> actorSessions = sessionsByActorKey.get(actorKey);
            if (actorSessions != null) {
                actorSessions.remove(sessionId);
                if (actorSessions.isEmpty()) {
                    sessionsByActorKey.remove(actorKey);
                }
            }

            if (removed.actorType() == ActorType.STAFF) {
                staffSessionIds.remove(sessionId);
                if (removed.userRole() != null) {
                    Set<String> roleSessions = sessionsByRole.get(removed.userRole());
                    if (roleSessions != null) {
                        roleSessions.remove(sessionId);
                        if (roleSessions.isEmpty()) {
                            sessionsByRole.remove(removed.userRole());
                        }
                    }
                }
            }
        }
    }

    public Set<String> findSessionIdsForActor(ActorType actorType, Long actorId) {
        if (actorType == null || actorId == null) {
            return Collections.emptySet();
        }
        Set<String> sessions = sessionsByActorKey.get(toActorKey(actorType, actorId));
        return sessions != null ? Collections.unmodifiableSet(sessions) : Collections.emptySet();
    }

    public Set<String> findPrincipalNamesForActor(ActorType actorType, Long actorId) {
        Set<String> sessionIds = findSessionIdsForActor(actorType, actorId);
        if (sessionIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> principalNames = ConcurrentHashMap.newKeySet();
        for (String sessionId : sessionIds) {
            SessionInfo info = sessionById.get(sessionId);
            if (info != null && info.principalName() != null) {
                principalNames.add(info.principalName());
            }
        }
        return Collections.unmodifiableSet(principalNames);
    }

    public Set<String> findStaffPrincipalNames() {
        if (staffSessionIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> principalNames = ConcurrentHashMap.newKeySet();
        for (String sessionId : staffSessionIds) {
            SessionInfo info = sessionById.get(sessionId);
            if (info != null && info.principalName() != null) {
                principalNames.add(info.principalName());
            }
        }
        return Collections.unmodifiableSet(principalNames);
    }

    public Set<String> findStaffSessionIds() {
        return Collections.unmodifiableSet(staffSessionIds);
    }

    public Set<String> findRoleSessionIds(UserRole role) {
        if (role == null) {
            return Collections.emptySet();
        }
        Set<String> sessions = sessionsByRole.get(role);
        return sessions != null ? Collections.unmodifiableSet(sessions) : Collections.emptySet();
    }

    public Set<String> findRolePrincipalNames(UserRole role) {
        if (role == null) {
            return Collections.emptySet();
        }
        Set<String> sessions = sessionsByRole.get(role);
        if (sessions == null || sessions.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> principalNames = ConcurrentHashMap.newKeySet();
        for (String sessionId : sessions) {
            SessionInfo info = sessionById.get(sessionId);
            if (info != null && info.principalName() != null) {
                principalNames.add(info.principalName());
            }
        }
        return Collections.unmodifiableSet(principalNames);
    }

    public SessionInfo getSession(String sessionId) {
        return sessionById.get(sessionId);
    }

    public static String toActorKey(ActorType actorType, Long actorId) {
        Objects.requireNonNull(actorType, "actorType must not be null");
        Objects.requireNonNull(actorId, "actorId must not be null");
        return actorType.name() + ":" + actorId;
    }
}
