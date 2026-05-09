<div align="center">

# 🎤 EchoBoard Backend API

### Real-time Q&A, live polling, moderation, and audience presence for interactive sessions

Built with **Spring Boot** · Secured with **JWT** · Powered by **PostgreSQL** · Real-time with **WebSocket/STOMP** · Redis-backed rate limiting, caching, presence, and poll counters · RabbitMQ-backed async mock email and analytics workflows · Scheduled maintenance jobs · Documented with **Swagger/OpenAPI**

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.14-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-JWT-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-010101?style=for-the-badge)](https://docs.spring.io/spring-framework/reference/web/websocket.html)
[![Swagger](https://img.shields.io/badge/Swagger-OpenAPI-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)](https://swagger.io/)
[![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Actuator](https://img.shields.io/badge/Actuator-Health%20Checks-6DB33F?style=for-the-badge)](https://docs.spring.io/spring-boot/reference/actuator/index.html)
[![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)](https://www.rabbitmq.com/)

</div>

---

## 📌 Overview

**EchoBoard** is a backend-focused real-time interaction platform for lectures, workshops, webinars, meetings, and live events.

A presenter creates a live session, shares a short access code with the audience, and participants can join without creating full user accounts. During the session, participants can submit questions, upvote approved questions, vote on live polls, and appear in live presence counts. Presenters manage the session lifecycle, moderate questions, publish polls, and receive real-time updates through WebSocket/STOMP topics.

The project is intentionally designed to be more than a CRUD API. It demonstrates production-style backend architecture with JWT authentication, refresh token rotation, participant-scoped tokens, ownership-based authorization, DTO contracts, service-layer business validation, PostgreSQL constraints, real-time WebSocket broadcasts, global error handling, Redis-backed rate limiting/caching/presence/live poll counters with scheduled PostgreSQL synchronization, RabbitMQ-backed asynchronous event processing with DLQ support, scheduled maintenance jobs, and a roadmap for Docker, analytics export, and integration testing.

Think of it as a backend-focused simplified version of **Slido** or **Mentimeter**, built to showcase real-time backend engineering rather than frontend screens.

---

## 🌐 Deployment Status

EchoBoard is currently documented as a local backend project. No production deployment URL was included in this repository snapshot.

| Environment | URL | Status |
|---|---|---|
| Local API | `http://localhost:8080` | Ready |
| Local Swagger UI | `http://localhost:8080/swagger-ui.html` | Ready |
| Local OpenAPI JSON | `http://localhost:8080/v3/api-docs` | Ready |
| Actuator Health | `http://localhost:8080/actuator/health` | Ready |
| RabbitMQ Dashboard | `http://localhost:15672` | Local only |
| Production API | `TODO` | Upcoming |
| Docker Compose | `TODO` | Planned |

> Production deployment, Docker Compose, CI, and full observability are planned roadmap items, not completed features in the current snapshot. Redis is implemented for rate limiting, access-code caching, presence tracking, and live poll option counters with scheduled PostgreSQL synchronization. RabbitMQ is implemented for asynchronous session-created and session-ended event flows with an analytics DLQ. Scheduled jobs are implemented for refresh token cleanup, long-running session auto-ending, old ended session archival, and Redis poll counter synchronization. Real email delivery, full analytics generation, retries with backoff, DLQ reprocessing, and advanced presence reconciliation remain future improvements.

---

## 📑 Table of Contents

- [Overview](#-overview)
- [Deployment Status](#-deployment-status)
- [Features](#-features)
- [Current Implementation Status](#-current-implementation-status)
- [Tech Stack](#-tech-stack)
- [Architecture Overview](#-architecture-overview)
- [REST Commands + WebSocket Broadcasts](#-rest-commands--websocket-broadcasts)
- [Authentication Flow](#-authentication-flow)
- [Presenter vs Participant Flow](#-presenter-vs-participant-flow)
- [Question Moderation Workflow](#-question-moderation-workflow)
- [Upvote and Duplicate Prevention](#-upvote-and-duplicate-prevention)
- [Live Polls Workflow](#-live-polls-workflow)
- [Redis Usage and Key Design](#-redis-usage-and-key-design)
- [RabbitMQ Async Processing](#-rabbitmq-async-processing)
- [Scheduled Jobs and Maintenance](#-scheduled-jobs-and-maintenance)
- [WebSocket Topics and Events](#-websocket-topics-and-events)
- [API Endpoints](#-api-endpoints)
- [Example API Requests & Responses](#-example-api-requests--responses)
- [Swagger Documentation](#-swagger-documentation)
- [Database Design](#-database-design)
- [Validation and Business Rules](#-validation-and-business-rules)
- [Error Handling](#-error-handling)
- [Testing](#-testing)
- [How to Run Locally](#-how-to-run-locally)
- [Configuration](#-configuration)
- [Database Setup Notes](#-database-setup-notes)
- [Key Design Decisions](#-key-design-decisions)
- [Screenshots and Demo Placeholders](#-screenshots-and-demo-placeholders)
- [Roadmap](#-roadmap)
- [What I Learned](#-what-i-learned)
- [Notes / Current Status](#-notes--current-status)
- [Author](#-author)

---

## ✨ Features

### 🔐 Authentication & Security

- Presenter registration and login with email and password
- Password hashing with **BCrypt**
- JWT access token generation and validation
- Refresh token persistence in PostgreSQL
- Refresh token rotation on refresh
- Logout flow through refresh token revocation
- Stateless authentication using **Spring Security**
- Current authenticated user endpoint with `/api/v1/users/me`
- Token type support for:
  - `ACCESS`
  - `REFRESH`
  - `PARTICIPANT`
- WebSocket authentication using Bearer tokens during STOMP `CONNECT`
- Refresh tokens are explicitly rejected for WebSocket connections

### 👤 Presenter Session Management

- Authenticated presenters can create sessions
- Each session receives a unique 6-character access code
- Sessions belong to an owner/presenter
- Owner-only session management is enforced in the service layer
- Presenter can:
  - create sessions
  - list their own sessions
  - get a session by ID
  - update editable sessions
  - start scheduled sessions
  - end live sessions
  - archive non-live sessions
  - delete scheduled sessions
- Session lifecycle is modeled with explicit status transitions

### 🙋 Participant Join Flow

- Participants join using a short session access code
- Participants can only join sessions that are currently `LIVE`
- Participants receive a limited participant JWT token
- Participant token contains `participantId`, `sessionId`, and token type
- Participant token hash is stored using SHA-256
- Anonymous participants are supported when the session allows anonymity
- Display name is required when anonymous access is disabled

### 📡 Real-Time WebSocket Foundation

- WebSocket endpoint available at `/ws`
- SockJS fallback enabled
- STOMP message broker configured with:
  - `/topic` for subscriptions
  - `/app` for application message mappings
- WebSocket authentication interceptor validates tokens on connection
- Live presence is tracked and broadcast to subscribed clients
- Disconnect handling updates live presence counts

### ❓ Questions and Moderation

- Participants submit questions using REST commands with participant tokens
- Redis rate limiting protects question submission from participant spam
- Questions are persisted before any real-time broadcast
- Moderation-enabled sessions create questions as `PENDING`
- Moderation-disabled sessions create questions directly as `APPROVED`
- Pending questions are broadcast to the moderation/pending topic
- Approved questions are broadcast to the public questions topic
- Session owner can:
  - approve pending questions
  - hide pending questions
  - pin approved questions
  - unpin approved questions
  - mark approved questions as answered
- Participants can delete only their own questions
- Deleted questions are broadcast to both public and pending topics

### 👍 Question Upvotes

- Participants can upvote approved questions
- Redis rate limiting protects upvote requests from participant spam
- Pending, hidden, or answered questions cannot be upvoted by the current implementation rules
- A participant can upvote the same question only once
- Duplicate upvotes are prevented by:
  - service-level existence checks
  - database unique constraint on `(question_id, participant_id)`
- Upvote count is persisted and broadcast as an updated question event

### 📊 Live Polls

- Presenters create polls as drafts
- Draft polls contain 2 to 10 options
- Poll types are modeled as:
  - `SINGLE_CHOICE`
  - `MULTIPLE_CHOICE`
- Session owner can publish draft polls
- Published polls are broadcast to the polls topic
- Participants can vote on published polls
- Duplicate poll votes are prevented by:
  - service-level existence checks
  - database unique constraint on `(poll_id, participant_id)`
- Poll results are updated and broadcast live after votes
- Published polls can be closed by the owner
- Only draft polls can be deleted

### ⚡ Redis Features

- Redis is integrated through Spring Data Redis and `StringRedisTemplate`
- Participant-scoped rate limiting is applied to:
  - question submissions
  - question upvote requests
- Rate limiting uses atomic Redis counters with TTL windows
- Session access-code lookups use cache-aside caching:
  - Redis stores `accessCode -> sessionId`
  - PostgreSQL remains the source of truth
  - cached entries use TTL to avoid stale mappings
- Presence tracking uses Redis counters and WebSocket session keys:
  - Redis stores online counts per session
  - Redis stores WebSocket session keys with TTL
  - leave/disconnect cleanup decrements counts and removes session keys
- Poll option vote counters are maintained in Redis for live results
- Poll responses and WebSocket poll events read live counts from Redis with PostgreSQL fallback
- Poll option `voteCount` is no longer updated on every vote; Redis serves live counts while PostgreSQL stores durable `PollVote` records
- A scheduled job periodically syncs Redis poll option counters back to PostgreSQL snapshots
- Closing a poll performs a final counter sync so closed poll results are persisted
- Redis key design is documented in the README

### 🐇 RabbitMQ Async Processing

- RabbitMQ is integrated through Spring AMQP
- The application declares RabbitMQ infrastructure from code:
  - `echoboard.exchange`
  - `email.queue`
  - `analytics.queue`
  - `echoboard.dlx`
  - `analytics.dlq`
- JSON message conversion is configured for RabbitMQ event payloads
- Session creation publishes a `SessionCreatedEvent`
- `EmailNotificationConsumer` consumes session-created events and logs a mock email notification
- Session ending publishes a `SessionEndedEvent`
- `AnalyticsConsumer` consumes session-ended events and logs mock analytics generation
- Failed analytics messages can be rejected without requeueing and routed to `analytics.dlq`
- RabbitMQ failure behavior was manually tested with DLQ routing
- Real email delivery, full analytics generation, retry backoff, idempotent consumers, and DLQ reprocessing are planned improvements

### ⏰ Scheduled Jobs and Maintenance

- Spring scheduling is enabled with `@EnableScheduling`
- Expired refresh tokens are removed automatically
- Revoked refresh tokens older than seven days are removed automatically
- Live sessions running longer than 24 hours are automatically ended as a safety cleanup
- Auto-ended sessions publish the same `SessionEndedEvent` used by manual session ending
- Ended sessions older than 30 days are automatically archived
- Redis live poll counters are synchronized back to PostgreSQL on a schedule
- Final poll counter synchronization runs when a published poll is closed
- Auto-end based on a user-defined scheduled end time is deferred until the product supports a `scheduledEndAt` style field
- Presence reconciliation and Redis key cleanup are planned future improvements

### 🛡️ Error Handling

- Centralized error handling with `@RestControllerAdvice`
- Custom `AppException` and `ErrorCode` model
- Consistent JSON error response with:
  - timestamp
  - HTTP status
  - error code
  - message
  - request path
  - validation fields when available
- Validation failures return structured field-level messages
- Database integrity conflicts return conflict-style duplicate resource errors

### 📖 API Documentation

- Swagger/OpenAPI dependencies are configured through Springdoc
- Swagger UI is available locally
- OpenAPI JSON is available through `/v3/api-docs`
- Actuator health and info endpoints are configured

---

## ✅ Current Implementation Status

| Area | Status | Notes |
|---|---:|---|
| Authentication | ✅ Implemented | Register, login, refresh token, logout, BCrypt, JWT |
| Refresh token rotation | ✅ Implemented | Old refresh token is revoked and replaced during refresh |
| Current user endpoint | ✅ Implemented | `GET /api/v1/users/me` |
| Session lifecycle | ✅ Implemented | Create, list, update, start, end, archive, delete |
| Access code generation | ✅ Implemented | 6-character uppercase alphanumeric code |
| Participant join | ✅ Implemented | Join live sessions by access code |
| Participant JWT tokens | ✅ Implemented | Limited participant tokens with session-scoped claims |
| WebSocket endpoint | ✅ Implemented | `/ws` with SockJS and STOMP |
| WebSocket auth | ✅ Implemented | Access and participant tokens accepted; refresh rejected |
| Presence | ✅ Implemented | Redis-backed counters and WebSocket session keys; heartbeat/reconciliation planned |
| Questions | ✅ Implemented | Submit, approve, hide, pin, unpin, answer, delete |
| Question upvotes | ✅ Implemented | Duplicate prevention with service check + DB constraint |
| Polls | ✅ Implemented | Draft, publish, vote, close, delete draft, Redis live counters with scheduled DB sync |
| Poll duplicate votes | ✅ Implemented | Duplicate prevention with service check + DB constraint |
| Global exception handling | ✅ Implemented | App errors, validation errors, integrity conflicts |
| Swagger/OpenAPI | ✅ Configured | Springdoc dependencies and Swagger UI path |
| Actuator | ✅ Configured | Health/info exposed |
| Redis | ✅ Implemented | Rate limiting, access-code cache, presence tracking, live poll counters, and scheduled counter sync |
| RabbitMQ | ✅ Implemented | Async session-created mock email flow, session-ended mock analytics flow, JSON conversion, and analytics DLQ |
| Docker Compose | 🟡 Planned | Redis and RabbitMQ are currently run locally through Docker; full app compose is planned |
| Analytics / CSV export | 🟡 Planned | Not implemented yet |
| Scheduled jobs | ✅ Implemented | Refresh token cleanup, long-running session auto-end, old ended session archive, and Redis poll counter sync |
| Integration tests | 🟡 Planned | Test dependencies exist; test classes were not included in the uploaded snapshot |

---

## 🛠️ Tech Stack

| Category | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.14 |
| Web | Spring Web MVC |
| Real-time | Spring WebSocket, STOMP, SockJS |
| Security | Spring Security, JWT, BCrypt |
| Token Library | JJWT 0.12.6 |
| Database | PostgreSQL |
| ORM | Spring Data JPA, Hibernate |
| Validation | Jakarta Validation |
| Mapping | MapStruct 1.6.3 |
| Boilerplate Reduction | Lombok |
| API Documentation | Springdoc OpenAPI / Swagger UI |
| Monitoring Foundation | Spring Boot Actuator |
| Build Tool | Maven |
| Testing Dependencies | JUnit 5, Spring Security Test, Testcontainers |
| Cache / Rate Limiting / Presence Counters | Redis |
| Async Processing | RabbitMQ, Spring AMQP |
| Scheduling | Spring Scheduling (`@Scheduled`) |

---

## 🏗️ Architecture Overview

EchoBoard follows a layered modular-monolith architecture. It stays simple enough to run as one Spring Boot application while still separating responsibilities clearly.

```text
Client / Swagger / Demo Frontend
      |
      v
Security Layer
SecurityConfig, JwtAuthenticationFilter, JwtService, WebSocketAuthInterceptor
      |
      v
Controller Layer
AuthController, SessionController, ParticipantController,
QuestionController, PollController, PresenceController, UserController
      |
      v
DTO + Mapper Layer
Request DTOs, Response DTOs, WebSocket Events, RabbitMQ Events, MapStruct Mappers
      |
      v
Service Layer
AuthService, SessionService, ParticipantService,
QuestionService, PollService, PresenceService,
RateLimiterService, SessionAccessCodeCacheService,
PollOptionCountCacheService, PollCounterSyncService, RabbitMQPublisher
      |
      v
Repository Layer
Spring Data JPA Repositories
      |
      v
PostgreSQL Database

Redis Layer
RateLimiterService, SessionAccessCodeCacheService,
PresenceService, PollOptionCountCacheService
      |
      v
Redis

RabbitMQ Layer
RabbitMQPublisher, EmailNotificationConsumer, AnalyticsConsumer
      |
      v
RabbitMQ
echoboard.exchange, email.queue, analytics.queue, analytics.dlq

Scheduler Layer
RefreshTokenCleanupJob, SessionAutoEndJob, SessionArchiveJob, PollCounterSyncJob
      |
      v
PostgreSQL + Redis + RabbitMQ
```