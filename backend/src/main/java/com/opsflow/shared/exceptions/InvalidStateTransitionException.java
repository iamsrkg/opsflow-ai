package com.opsflow.shared.exceptions;

public class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(String currentStatus, String targetStatus, String reason) {
        super(String.format("Invalid state transition from %s to %s. Reason: %s", currentStatus, targetStatus, reason));
    }
}
