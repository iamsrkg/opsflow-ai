# Event-Driven Design

## Event envelope

The event envelope carries:

- `eventId` — globally unique event identity;
- `eventType` — semantic event name;
- `aggregateType` — source aggregate;
- `aggregateId` — aggregate identity;
- `correlationId` — request/trace correlation;
- `occurredAt` — event timestamp;
- `version` — schema version;
- `payload` — serialized business data.

## Example

```json
{
  "eventId": "uuid",
  "eventType": "REQUEST_CREATED",
  "aggregateType": "REQUEST",
  "aggregateId": "uuid",
  "correlationId": "uuid",
  "occurredAt": "2026-10-10T08:00:00Z",
  "version": 1,
  "payload": "{...}"
}
```

## Delivery semantics

The project intentionally demonstrates **at-least-once processing** at the application level.

The producer uses Kafka idempotence (`enable.idempotence=true`, `acks=all`) but this does not magically make the whole business workflow exactly-once. The database and Kafka are different systems.

The application therefore assumes duplicates can happen and protects side effects using processed-event tracking/idempotent handlers.

## Ordering

Kafka ordering is guaranteed within a partition, not globally across the topic. OpsFlow publishes using the request aggregate ID as the Kafka key so events for the same request are routed consistently to the same partition.

## Retry and DLT

A transient consumer failure should be retried. A poison message should eventually be routed to a dead-letter topic so it does not block the partition indefinitely.

## Event evolution

Never silently change the meaning of an existing event version. Add fields compatibly or publish a new schema/event version when semantics change.
