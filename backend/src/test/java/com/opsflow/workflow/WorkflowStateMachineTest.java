package com.opsflow.workflow;

import com.opsflow.request.RequestStatus;
import com.opsflow.shared.exceptions.InvalidStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowStateMachineTest {

    private WorkflowStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new WorkflowStateMachine();
    }

    @Test
    void testValidTransitions() {
        assertTrue(stateMachine.isTransitionAllowed(RequestStatus.CREATED, RequestStatus.CLASSIFIED));
        assertTrue(stateMachine.isTransitionAllowed(RequestStatus.CLASSIFIED, RequestStatus.ASSIGNED));
        assertTrue(stateMachine.isTransitionAllowed(RequestStatus.ASSIGNED, RequestStatus.INVESTIGATING));
        assertTrue(stateMachine.isTransitionAllowed(RequestStatus.INVESTIGATING, RequestStatus.PENDING_APPROVAL));
        assertTrue(stateMachine.isTransitionAllowed(RequestStatus.PENDING_APPROVAL, RequestStatus.ACTION_IN_PROGRESS));
        assertTrue(stateMachine.isTransitionAllowed(RequestStatus.ACTION_IN_PROGRESS, RequestStatus.RESOLVED));
    }

    @Test
    void testInvalidTransitionsThrowException() {
        assertThrows(InvalidStateTransitionException.class, () ->
                stateMachine.validateTransition(RequestStatus.CREATED, RequestStatus.RESOLVED));

        assertThrows(InvalidStateTransitionException.class, () ->
                stateMachine.validateTransition(RequestStatus.CLASSIFIED, RequestStatus.CLOSED));
    }
}
