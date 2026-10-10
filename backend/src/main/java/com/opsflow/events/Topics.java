package com.opsflow.events;

/** Kafka topic names. One topic per aggregate type, keyed by aggregate id so one request's events stay in order. */
public final class Topics {

    public static final String REQUEST = "opsflow.request.events";
    public static final String INVESTIGATION = "opsflow.investigation.events";
    public static final String APPROVAL = "opsflow.approval.events";

    /** Where a message goes after its retries are used up. */
    public static final String DEAD_LETTER_SUFFIX = ".DLT";

    private Topics() {
    }

    public static String forAggregate(String aggregateType) {
        return "opsflow." + aggregateType.toLowerCase() + ".events";
    }
}
