# Threat Model

## Assets

- request/customer operational data;
- authentication credentials;
- JWT signing secret;
- AI provider credentials;
- knowledge documents;
- audit history;
- approval decisions.

## Actors

- normal operator;
- manager/admin;
- compromised account;
- malicious API client;
- malicious/untrusted request text;
- compromised dependency.

## Threats

### Credential theft

Mitigations: password hashing, short token TTL, TLS, secret management, key rotation.

### Broken authorization

Mitigations: Spring Security, role checks, future resource-level policies, negative tests.

### Prompt injection

Mitigations: treat model/user/retrieved text as untrusted, bound evidence, deterministic authorization.

### Replay/duplicate requests

Mitigations: API idempotency keys and unique constraints.

### Duplicate events

Mitigations: processed event IDs and idempotent handlers.

### Data exfiltration through AI

Mitigations: backend-controlled evidence selection and authorization before tool access.

### Supply-chain risk

Mitigations: dependency scanning, pinned versions where practical, CI security gates.

## Interview principle

Security is not one filter. It is defense in depth across authentication, authorization, validation, data access, event handling, AI boundaries and auditability.
