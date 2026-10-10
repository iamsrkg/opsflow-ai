# Technology Decision Record

## Java 21

**Why:** modern LTS Java, strong ecosystem, records/pattern matching and excellent Spring support.

**Alternative:** Node.js/TypeScript. The project intentionally demonstrates enterprise Java backend depth.

## Spring Boot

**Why:** mature dependency injection, web, security, JPA, Kafka, observability and testing ecosystem.

## PostgreSQL

**Why:** transactional relational workload plus pgvector.

## Kafka

**Why:** durable event stream, partitions, replay and multiple consumers.

## Redis

**Why:** fast ephemeral/cache/rate-limit state.

## React + TypeScript

**Why:** enough UI to demonstrate the backend without turning the project into a frontend project.

## pgvector

**Why:** vector retrieval without introducing a second specialized vector database at moderate scale.

## Modular monolith

**Why:** lower operational cost while preserving domain boundaries.

## JWT

**Why:** simple stateless API authentication for the portfolio deployment.

## AI provider abstraction

**Why:** vendor independence, deterministic testing and easier local setup.

## Human approval

**Why:** AI output is probabilistic; protected actions require deterministic authorization and accountability.
