package com.opsflow.workflow;

import com.opsflow.request.RequestStatus;
import com.opsflow.shared.exceptions.InvalidStateTransitionException;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class WorkflowStateMachine {

    private static final Map<RequestStatus, Set<RequestStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(RequestStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(RequestStatus.CREATED, Set.of(RequestStatus.CLASSIFIED, RequestStatus.ASSIGNED));
        ALLOWED_TRANSITIONS.put(RequestStatus.CLASSIFIED, Set.of(RequestStatus.ASSIGNED, RequestStatus.INVESTIGATING, RequestStatus.ESCALATED));
        ALLOWED_TRANSITIONS.put(RequestStatus.ASSIGNED, Set.of(RequestStatus.INVESTIGATING, RequestStatus.ESCALATED));
        ALLOWED_TRANSITIONS.put(RequestStatus.INVESTIGATING, Set.of(RequestStatus.PENDING_APPROVAL, RequestStatus.ACTION_IN_PROGRESS, RequestStatus.RESOLVED, RequestStatus.WAITING_FOR_USER, RequestStatus.ESCALATED));
        ALLOWED_TRANSITIONS.put(RequestStatus.WAITING_FOR_USER, Set.of(RequestStatus.INVESTIGATING, RequestStatus.RESOLVED));
        ALLOWED_TRANSITIONS.put(RequestStatus.PENDING_APPROVAL, Set.of(RequestStatus.ACTION_IN_PROGRESS, RequestStatus.INVESTIGATING, RequestStatus.RESOLVED));
        ALLOWED_TRANSITIONS.put(RequestStatus.ACTION_IN_PROGRESS, Set.of(RequestStatus.RESOLVED, RequestStatus.ESCALATED, RequestStatus.INVESTIGATING));
        ALLOWED_TRANSITIONS.put(RequestStatus.ESCALATED, Set.of(RequestStatus.INVESTIGATING, RequestStatus.ASSIGNED));
        ALLOWED_TRANSITIONS.put(RequestStatus.RESOLVED, Set.of(RequestStatus.CLOSED, RequestStatus.INVESTIGATING));
        ALLOWED_TRANSITIONS.put(RequestStatus.CLOSED, Set.of(RequestStatus.INVESTIGATING)); // Re-open
    }

    public boolean isTransitionAllowed(RequestStatus from, RequestStatus to) {
        if (from == to) return true;
        Set<RequestStatus> allowed = ALLOWED_TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    public void validateTransition(RequestStatus from, RequestStatus to) {
        if (!isTransitionAllowed(from, to)) {
            throw new InvalidStateTransitionException(
                    from.name(), to.name(),
                    String.format("Transition from %s to %s is not permitted by workflow business rules.", from, to)
            );
        }
    }
}
