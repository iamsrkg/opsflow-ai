# ADR-001: Modular Monolith First

Status: Accepted

## Decision
Build OpsFlow as a modular Spring Boot application before extracting independently deployable services.

## Why
- Faster iteration and lower operational complexity.
- Clear domain boundaries remain visible in packages.
- Database transactions are simpler where strong consistency matters.
- Extraction can be driven by actual scaling/ownership evidence.

## Trade-off
The application is initially a single deployment unit. This is acceptable for the project and can evolve when measured bottlenecks justify extraction.
