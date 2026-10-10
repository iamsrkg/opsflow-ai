# Production Hardening Checklist

- [ ] Flyway migrations instead of `ddl-auto=update`
- [ ] Secrets in AWS Secrets Manager/Kubernetes Secret Store
- [ ] TLS everywhere
- [ ] JWT signing-key rotation and refresh-token/session revocation
- [ ] Resource-level authorization and tenant isolation
- [ ] Kafka replication >= 3 in production
- [ ] PostgreSQL HA/backups/PITR
- [ ] Redis HA
- [ ] OpenTelemetry traces + centralized logs
- [ ] DLT replay tooling
- [ ] k6 performance baseline
- [ ] SAST/DAST/dependency scanning
- [ ] AI evaluation dataset and prompt/version tracking
- [ ] PII redaction before model calls
- [ ] Cost/token monitoring
- [ ] Disaster recovery drill
