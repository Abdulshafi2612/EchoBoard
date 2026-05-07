<div align="center">

# 🎤 EchoBoard Backend API

### Real-time Q&A, live polling, moderation, and audience presence for interactive sessions

Built with **Spring Boot** · Secured with **JWT** · Powered by **PostgreSQL** · Real-time with **WebSocket/STOMP** · Documented with **Swagger/OpenAPI**

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.14-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-JWT-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-010101?style=for-the-badge)](https://docs.spring.io/spring-framework/reference/web/websocket.html)
[![Swagger](https://img.shields.io/badge/Swagger-OpenAPI-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)](https://swagger.io/)
[![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Actuator](https://img.shields.io/badge/Actuator-Health%20Checks-6DB33F?style=for-the-badge)](https://docs.spring.io/spring-boot/reference/actuator/index.html)
[![Redis](https://img.shields.io/badge/Redis-Planned-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-Planned-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)](https://www.rabbitmq.com/)

</div>

---

## 📌 Overview

**EchoBoard** is a backend-focused real-time interaction platform for lectures, workshops, webinars, meetings, and live events.

A presenter creates a live session, shares a short access code with the audience, and participants can join without creating full user accounts. During the session, participants can submit questions, upvote approved questions, vote on live polls, and appear in live presence counts. Presenters manage the session lifecycle, moderate questions, publish polls, and receive real-time updates through WebSocket/STOMP topics.

The project is intentionally designed to be more than a CRUD API. It demonstrates production-style backend architecture with JWT authentication, refresh token rotation, participant-scoped tokens, ownership-based authorization, DTO contracts, service-layer business validation, PostgreSQL constraints, real-time WebSocket broadcasts, global error handling, and a roadmap for Redis, RabbitMQ, Docker, analytics, and integration testing.

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
| Production API | `TODO` | Upcoming |
| Docker Compose | `TODO` | Planned |

> Production deployment, Docker Compose, Redis, RabbitMQ, CI, and full observability are planned roadmap items, not completed features in the current snapshot.

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
| Presence | ✅ Implemented | In-memory single-instance presence counts |
| Questions | ✅ Implemented | Submit, approve, hide, pin, unpin, answer, delete |
| Question upvotes | ✅ Implemented | Duplicate prevention with service check + DB constraint |
| Polls | ✅ Implemented | Draft, publish, vote, close, delete draft |
| Poll duplicate votes | ✅ Implemented | Duplicate prevention with service check + DB constraint |
| Global exception handling | ✅ Implemented | App errors, validation errors, integrity conflicts |
| Swagger/OpenAPI | ✅ Configured | Springdoc dependencies and Swagger UI path |
| Actuator | ✅ Configured | Health/info exposed |
| Redis | 🟡 Planned | Dependency currently commented out |
| RabbitMQ | 🟡 Planned | Runtime dependency currently commented out |
| Docker Compose | 🟡 Planned | Not included in current snapshot |
| Analytics / CSV export | 🟡 Planned | Not implemented yet |
| Scheduled jobs | 🟡 Planned | Not implemented yet |
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
| Planned Cache / Rate Limiting | Redis |
| Planned Async Processing | RabbitMQ |

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
Request DTOs, Response DTOs, WebSocket Events, MapStruct Mappers
      |
      v
Service Layer
AuthService, SessionService, ParticipantService,
QuestionService, PollService, PresenceService
      |
      v
Repository Layer
Spring Data JPA Repositories
      |
      v
PostgreSQL Database
```

### Package Structure

```text
com.echoboard
├── config          # Security and WebSocket configuration
├── controller      # REST controllers and WebSocket message controllers
├── dto             # Request DTOs, response DTOs, event payloads
├── entity          # JPA entities
├── enums           # Domain statuses, roles, token types, event types
├── exception       # AppException, ErrorCode, ErrorResponse, global handler
├── mapper          # MapStruct mappers
├── repository      # Spring Data JPA repositories
├── security        # JWT, filters, user details, WebSocket auth
├── service         # Service interfaces
├── service.impl    # Business logic implementations
└── util            # Access code generator and SHA-256 token hashing
```

### Why This Architecture Works

Controllers stay thin. They receive requests and delegate to services. Services own the business rules: ownership checks, status transitions, participant validation, muted-user checks, duplicate vote checks, and WebSocket broadcasting after persistence. Repositories focus only on database access. DTOs keep entities from leaking directly into API responses.

---

## 🔁 REST Commands + WebSocket Broadcasts

One of the most important design decisions in EchoBoard is the separation between **commands** and **real-time broadcasts**.

### Command Side

Database-changing actions are handled through REST endpoints:

- register and login
- create/start/end/archive sessions
- join a session
- submit questions
- approve/hide/pin/answer/delete questions
- upvote questions
- create/publish/close/delete polls
- vote on polls

REST is used because these operations need predictable HTTP status codes, validation errors, authentication, authorization, transactions, and a clean request/response contract.

### Broadcast Side

After a command successfully mutates database state, the service layer broadcasts a WebSocket/STOMP event to subscribed clients.

```text
REST Command
      |
      v
Controller
      |
      v
Service Layer Validation
authorization, ownership, status checks, duplicate checks
      |
      v
PostgreSQL Transaction
persist question / vote / poll / session changes
      |
      v
SimpMessagingTemplate
      |
      v
WebSocket Topic Broadcast
/topic/sessions/{sessionId}/...
```

### Why Not Send Every Command Through WebSocket?

Using WebSocket for every command can make validation, error handling, retries, authorization, and API documentation harder to reason about. EchoBoard keeps WebSocket focused on what it does best: pushing events to connected clients in real time.

This split makes the backend easier to test, easier to document, and closer to a production-style command/event architecture.

---

## 🔐 Authentication Flow

EchoBoard uses JWT-based stateless authentication for presenters and separate participant tokens for audience members.

### Presenter Authentication

1. Presenter registers using name, email, and password.
2. Password is hashed with BCrypt before saving.
3. Presenter logs in with email and password.
4. Backend returns:
   - `accessToken`
   - `refreshToken`
   - `tokenType: Bearer`
5. Protected presenter endpoints require:

```http
Authorization: Bearer <accessToken>
```

6. When the access token expires, the client refreshes it with the refresh token.
7. Refresh flow revokes the old refresh token and issues a new access/refresh token pair.
8. Logout revokes the submitted refresh token.

### Participant Authentication

1. Presenter starts a session.
2. Participant joins using the session access code.
3. Backend creates a participant record.
4. Backend returns a `participantToken`.
5. Participant actions require:

```http
Authorization: Bearer <participantToken>
```

Participant tokens are limited. They carry `participantId`, `sessionId`, and `type: PARTICIPANT`, which allows the backend to verify that the participant belongs to the target session.

### Token Types

| Token Type | Used For | Accepted By WebSocket |
|---|---|---:|
| `ACCESS` | Presenter API access | Yes |
| `REFRESH` | Refreshing access tokens | No |
| `PARTICIPANT` | Audience actions inside one session | Yes |

---

## 👥 Presenter vs Participant Flow

### Presenter Flow

```text
Register / Login
      |
      v
Create Session
      |
      v
Start Session
      |
      v
Share Access Code
      |
      v
Moderate Questions + Publish Polls
      |
      v
End / Archive Session
```

### Participant Flow

```text
Enter Access Code
      |
      v
Join Live Session
      |
      v
Receive Participant Token
      |
      v
Submit Questions / Upvote / Vote on Polls
      |
      v
Receive Live Updates through WebSocket Topics
```

### Current Role Model

| Actor | Current Capabilities |
|---|---|
| Presenter | Owns sessions, manages lifecycle, moderates questions, manages polls |
| Participant | Joins live sessions, submits questions, deletes own questions, upvotes, votes on polls |
| Admin | Enum exists, but no platform-level admin workflows are implemented yet |
| Moderator | Planned concept; current implementation uses session owner as moderator |

---

## ❓ Question Moderation Workflow

Questions are stored in PostgreSQL before being broadcast. The initial status depends on the session moderation setting.

### Moderation Enabled

```text
Participant submits question
      |
      v
Question saved as PENDING
      |
      v
Broadcast to /topic/sessions/{sessionId}/questions/pending
      |
      v
Presenter approves or hides
      |
      +--> Approved: broadcast to public questions topic
      |
      +--> Hidden: update pending topic only
```

### Moderation Disabled

```text
Participant submits question
      |
      v
Question saved as APPROVED
      |
      v
Broadcast to /topic/sessions/{sessionId}/questions
```

### Question Statuses

| Status | Meaning |
|---|---|
| `PENDING` | Waiting for presenter approval |
| `APPROVED` | Publicly visible and eligible for upvotes, pinning, and answering |
| `HIDDEN` | Rejected/hidden from the public feed |
| `ANSWERED` | Marked as answered by presenter |

### Question Event Types

| Event Type | Meaning |
|---|---|
| `CREATED` | A new question was submitted |
| `UPDATED` | Question status, pin state, answered state, or upvote count changed |
| `DELETED` | A participant deleted their own question |

---

## 👍 Upvote and Duplicate Prevention

Upvotes are implemented as a database-backed participant action.

```text
Participant sends REST upvote command
      |
      v
Validate participant token
      |
      v
Validate live session
      |
      v
Validate participant belongs to session
      |
      v
Validate participant is not muted
      |
      v
Validate question belongs to session
      |
      v
Validate question is APPROVED
      |
      v
Check question_votes for existing vote
      |
      v
Insert QuestionVote + increment upvoteCount
      |
      v
Broadcast UPDATED QuestionEvent
```

### Duplicate Protection

| Layer | Protection |
|---|---|
| Service layer | Checks whether a vote already exists for participant + question |
| Database layer | Unique constraint on `(question_id, participant_id)` |

This protects against normal duplicate requests and also provides a database-level safety net for concurrent requests.

---

## 📊 Live Polls Workflow

Polls are presenter-managed and participant-voted.

```text
Presenter creates draft poll
      |
      v
Poll saved as DRAFT with options
      |
      v
Presenter publishes poll during LIVE session
      |
      v
Broadcast PUBLISHED PollEvent
      |
      v
Participants vote with participant token
      |
      v
Vote saved + option count incremented
      |
      v
Broadcast UPDATED PollEvent with live results
      |
      v
Presenter closes poll
      |
      v
Broadcast CLOSED PollEvent
```

### Poll Statuses

| Status | Meaning |
|---|---|
| `DRAFT` | Created by presenter but not visible as a published poll |
| `PUBLISHED` | Open for participant voting |
| `CLOSED` | No longer accepts votes |

### Poll Event Types

| Event Type | Meaning |
|---|---|
| `PUBLISHED` | Poll became visible to clients |
| `UPDATED` | Poll result counts changed after a vote |
| `CLOSED` | Poll was closed by the presenter |
| `DELETED` | Enum exists, but draft deletion currently does not broadcast a public delete event |

### Important Poll Note

`MULTIPLE_CHOICE` exists as a poll type enum. However, the current vote model has a unique constraint on `(poll_id, participant_id)`, so each participant can currently submit only one vote per poll. Full multiple-choice behavior is a planned refinement.

---

## 📡 WebSocket Topics and Events

### Connection Endpoint

```text
/ws
```

Clients connect using STOMP over SockJS and include an Authorization header:

```http
Authorization: Bearer <accessToken-or-participantToken>
```

### Broker Configuration

| Prefix | Purpose |
|---|---|
| `/topic` | Client subscriptions for broadcast events |
| `/app` | Client messages handled by server-side `@MessageMapping` methods |

### Client Subscriptions

| Topic | Audience | Purpose |
|---|---|---|
| `/topic/sessions/{sessionId}/questions` | Public session clients | Approved questions, updates, deletions |
| `/topic/sessions/{sessionId}/questions/pending` | Presenter/moderation UI | Pending questions and moderation updates |
| `/topic/sessions/{sessionId}/polls` | Public session clients | Poll published, result updated, poll closed |
| `/topic/sessions/{sessionId}/presence` | Public/presenter clients | Live online count updates |

### Client Message Destinations

| Destination | Purpose |
|---|---|
| `/app/sessions/{sessionId}/presence.join` | Join live presence tracking |
| `/app/sessions/presence.leave` | Leave live presence tracking |

> Questions, upvotes, poll creation, poll publishing, poll voting, and moderation actions are currently REST commands. WebSocket is used to broadcast the resulting state changes.

### Example Question Event

```json
{
  "questionEventType": "UPDATED",
  "questionId": 12,
  "sessionId": 3,
  "participantDisplayName": "Anonymous",
  "content": "Can you explain the WebSocket flow again?",
  "status": "APPROVED",
  "upvoteCount": 5,
  "pinned": false,
  "answered": false,
  "createdAt": "2026-05-07T14:20:00",
  "approvedAt": "2026-05-07T14:21:00",
  "answeredAt": null
}
```

### Example Question Deleted Event

```json
{
  "questionEventType": "DELETED",
  "questionId": 12,
  "sessionId": 3
}
```

### Example Poll Event

```json
{
  "eventType": "UPDATED",
  "id": 7,
  "sessionId": 3,
  "title": "Which topic should we cover next?",
  "status": "PUBLISHED",
  "type": "SINGLE_CHOICE",
  "options": [
    {
      "id": 21,
      "text": "Redis rate limiting",
      "voteCount": 8
    },
    {
      "id": 22,
      "text": "RabbitMQ analytics",
      "voteCount": 4
    }
  ],
  "createdAt": "2026-05-07T14:10:00",
  "publishedAt": "2026-05-07T14:15:00",
  "closedAt": null
}
```

### Example Presence Event

```json
{
  "sessionId": 3,
  "type": "JOINED",
  "onlineCount": 24
}
```

---

## 📡 API Endpoints

### Base URLs

| Environment | Base URL |
|---|---|
| Local | `http://localhost:8080` |
| Production | `TODO` |

### Authentication

| Method | Endpoint | Description | Auth Required |
|---|---|---|---:|
| POST | `/api/v1/auth/register` | Register a presenter account | No |
| POST | `/api/v1/auth/login` | Login and receive access/refresh tokens | No |
| POST | `/api/v1/auth/refresh-token` | Rotate refresh token and receive new tokens | No |
| POST | `/api/v1/auth/logout` | Revoke a refresh token | Yes |

### Users

| Method | Endpoint | Description | Auth Required |
|---|---|---|---:|
| GET | `/api/v1/users/me` | Get current authenticated presenter profile | Yes |

### Sessions

| Method | Endpoint | Description | Auth Required |
|---|---|---|---:|
| POST | `/api/v1/sessions` | Create a new session | Yes |
| GET | `/api/v1/sessions/my` | Get paginated sessions owned by current presenter | Yes |
| GET | `/api/v1/sessions/{id}` | Get owned session by ID | Yes |
| PUT | `/api/v1/sessions/{id}` | Update an owned editable session | Yes |
| PATCH | `/api/v1/sessions/{id}/start` | Start a scheduled session | Yes |
| PATCH | `/api/v1/sessions/{id}/end` | End a live session | Yes |
| PATCH | `/api/v1/sessions/{id}/archive` | Archive a non-live session | Yes |
| DELETE | `/api/v1/sessions/{id}` | Delete a scheduled session | Yes |
| POST | `/api/v1/sessions/join` | Join a live session by access code | No |

### Questions

| Method | Endpoint | Description | Auth Required |
|---|---|---|---:|
| GET | `/api/v1/sessions/{sessionId}/questions?status=PENDING` | Get paginated questions by status for owned live session | Yes |
| POST | `/api/v1/sessions/{sessionId}/questions` | Submit a question with participant token | Participant token |
| PATCH | `/api/v1/sessions/{sessionId}/questions/{questionId}/approve` | Approve a pending question | Yes |
| PATCH | `/api/v1/sessions/{sessionId}/questions/{questionId}/hide` | Hide a pending question | Yes |
| PATCH | `/api/v1/sessions/{sessionId}/questions/{questionId}/pin` | Pin an approved question | Yes |
| PATCH | `/api/v1/sessions/{sessionId}/questions/{questionId}/unpin` | Unpin an approved question | Yes |
| PATCH | `/api/v1/sessions/{sessionId}/questions/{questionId}/answer` | Mark an approved question as answered | Yes |
| POST | `/api/v1/sessions/{sessionId}/questions/{questionId}/upvote` | Upvote an approved question | Participant token |
| DELETE | `/api/v1/sessions/{sessionId}/questions/{questionId}` | Delete participant-owned question | Participant token |

### Polls

| Method | Endpoint | Description | Auth Required |
|---|---|---|---:|
| POST | `/api/v1/sessions/{sessionId}/polls` | Create a draft poll | Yes |
| PATCH | `/api/v1/sessions/{sessionId}/polls/{pollId}/publish` | Publish a draft poll | Yes |
| PATCH | `/api/v1/sessions/{sessionId}/polls/{pollId}/close` | Close a published poll | Yes |
| POST | `/api/v1/sessions/{sessionId}/polls/{pollId}/vote/{optionId}` | Vote on a published poll | Participant token |
| DELETE | `/api/v1/sessions/{sessionId}/polls/{pollId}` | Delete a draft poll | Yes |

### WebSocket

| Type | Destination | Purpose |
|---|---|---|
| CONNECT | `/ws` | Connect using Bearer access or participant token |
| SUBSCRIBE | `/topic/sessions/{sessionId}/questions` | Public question events |
| SUBSCRIBE | `/topic/sessions/{sessionId}/questions/pending` | Pending/moderation question events |
| SUBSCRIBE | `/topic/sessions/{sessionId}/polls` | Poll events and live result updates |
| SUBSCRIBE | `/topic/sessions/{sessionId}/presence` | Presence count updates |
| SEND | `/app/sessions/{sessionId}/presence.join` | Join live presence |
| SEND | `/app/sessions/presence.leave` | Leave live presence |

---

## 📋 Example API Requests & Responses

### Register Presenter

```http
POST /api/v1/auth/register
Content-Type: application/json
```

```json
{
  "name": "Mohamed",
  "email": "mohamed@test.com",
  "password": "12345678"
}
```

```json
{
  "id": 1,
  "name": "Mohamed",
  "email": "mohamed@test.com",
  "role": "PRESENTER",
  "message": "Account created successfully"
}
```

### Login

```http
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{
  "email": "mohamed@test.com",
  "password": "12345678"
}
```

```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi...",
  "tokenType": "Bearer"
}
```

### Refresh Token

```http
POST /api/v1/auth/refresh-token
Content-Type: application/json
```

```json
{
  "refreshToken": "eyJhbGciOi..."
}
```

### Logout

```http
POST /api/v1/auth/logout
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "refreshToken": "eyJhbGciOi..."
}
```

```json
{
  "message": "Logged out successfully"
}
```

### Create Session

```http
POST /api/v1/sessions
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "title": "Backend Architecture Workshop",
  "description": "Live Q&A and polling for the Spring Boot session",
  "moderationEnabled": true,
  "anonymousAllowed": true
}
```

```json
{
  "id": 1,
  "title": "Backend Architecture Workshop",
  "description": "Live Q&A and polling for the Spring Boot session",
  "accessCode": "X7K29A",
  "status": "SCHEDULED",
  "moderationEnabled": true,
  "anonymousAllowed": true,
  "ownerName": "Mohamed",
  "ownerId": 1,
  "createdAt": "2026-05-07T14:00:00",
  "startedAt": null,
  "endedAt": null
}
```

### Start Session

```http
PATCH /api/v1/sessions/1/start
Authorization: Bearer <accessToken>
```

```json
{
  "id": 1,
  "title": "Backend Architecture Workshop",
  "accessCode": "X7K29A",
  "status": "LIVE",
  "moderationEnabled": true,
  "anonymousAllowed": true,
  "ownerName": "Mohamed",
  "ownerId": 1,
  "createdAt": "2026-05-07T14:00:00",
  "startedAt": "2026-05-07T14:05:00",
  "endedAt": null
}
```

### Join Session as Participant

```http
POST /api/v1/sessions/join
Content-Type: application/json
```

```json
{
  "accessCode": "X7K29A",
  "displayName": "Ahmed"
}
```

```json
{
  "id": 10,
  "sessionId": 1,
  "displayName": "Ahmed",
  "participantToken": "eyJhbGciOi...",
  "joinedAt": "2026-05-07T14:06:00"
}
```

### Submit Question

```http
POST /api/v1/sessions/1/questions
Authorization: Bearer <participantToken>
Content-Type: application/json
```

```json
{
  "content": "Why did you separate REST commands from WebSocket broadcasts?"
}
```

```http
201 Created
```

The created question is then broadcast as either a pending or public `QuestionEvent`, depending on the session moderation setting.

### Approve Question

```http
PATCH /api/v1/sessions/1/questions/12/approve
Authorization: Bearer <accessToken>
```

```http
200 OK
```

The approved question is broadcast to the public questions topic.

### Upvote Question

```http
POST /api/v1/sessions/1/questions/12/upvote
Authorization: Bearer <participantToken>
```

```http
200 OK
```

The updated question is broadcast with the new `upvoteCount`.

### Create Draft Poll

```http
POST /api/v1/sessions/1/polls
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "title": "Which backend topic should we go deeper into?",
  "type": "SINGLE_CHOICE",
  "options": [
    {
      "text": "Redis rate limiting"
    },
    {
      "text": "RabbitMQ async processing"
    },
    {
      "text": "Testcontainers integration tests"
    }
  ]
}
```

```json
{
  "id": 7,
  "sessionId": 1,
  "title": "Which backend topic should we go deeper into?",
  "status": "DRAFT",
  "type": "SINGLE_CHOICE",
  "options": [
    {
      "id": 21,
      "text": "Redis rate limiting",
      "voteCount": 0
    },
    {
      "id": 22,
      "text": "RabbitMQ async processing",
      "voteCount": 0
    },
    {
      "id": 23,
      "text": "Testcontainers integration tests",
      "voteCount": 0
    }
  ],
  "createdAt": "2026-05-07T14:10:00",
  "publishedAt": null,
  "closedAt": null
}
```

### Publish Poll

```http
PATCH /api/v1/sessions/1/polls/7/publish
Authorization: Bearer <accessToken>
```

```json
{
  "id": 7,
  "sessionId": 1,
  "title": "Which backend topic should we go deeper into?",
  "status": "PUBLISHED",
  "type": "SINGLE_CHOICE",
  "options": [
    {
      "id": 21,
      "text": "Redis rate limiting",
      "voteCount": 0
    }
  ],
  "createdAt": "2026-05-07T14:10:00",
  "publishedAt": "2026-05-07T14:12:00",
  "closedAt": null
}
```

### Vote on Poll

```http
POST /api/v1/sessions/1/polls/7/vote/21
Authorization: Bearer <participantToken>
```

```json
{
  "id": 7,
  "sessionId": 1,
  "title": "Which backend topic should we go deeper into?",
  "status": "PUBLISHED",
  "type": "SINGLE_CHOICE",
  "options": [
    {
      "id": 21,
      "text": "Redis rate limiting",
      "voteCount": 1
    },
    {
      "id": 22,
      "text": "RabbitMQ async processing",
      "voteCount": 0
    }
  ],
  "createdAt": "2026-05-07T14:10:00",
  "publishedAt": "2026-05-07T14:12:00",
  "closedAt": null
}
```

---

## 📖 Swagger Documentation

### Local Swagger UI

```text
http://localhost:8080/swagger-ui.html
```

Depending on Springdoc routing, this may also redirect to:

```text
http://localhost:8080/swagger-ui/index.html
```

### Local OpenAPI JSON

```text
http://localhost:8080/v3/api-docs
```

### Health Check

```text
http://localhost:8080/actuator/health
```

Swagger is useful for exploring REST endpoints. For WebSocket testing, use a STOMP/SockJS client or a small demo frontend.

---

## 🗄️ Database Design

### Main Entities

| Entity | Table | Purpose |
|---|---|---|
| `User` | `users` | Presenter/admin accounts |
| `RefreshToken` | `refresh_tokens` | Persistent refresh tokens with revocation state |
| `Session` | `sessions` | Live event sessions owned by presenters |
| `Participant` | `participants` | Audience members joined to a session |
| `Question` | `questions` | Participant-submitted questions |
| `QuestionVote` | `question_votes` | Question upvotes by participants |
| `Poll` | `polls` | Presenter-created polls |
| `PollOption` | `poll_options` | Options inside a poll |
| `PollVote` | `poll_votes` | Participant poll votes |

### Relationships

| Relationship | Type |
|---|---|
| User → Sessions | One-to-Many |
| User → RefreshTokens | One-to-Many |
| Session → Participants | One-to-Many |
| Session → Questions | One-to-Many |
| Session → Polls | One-to-Many |
| Participant → Questions | One-to-Many |
| Question → QuestionVotes | One-to-Many |
| Poll → PollOptions | One-to-Many |
| Poll → PollVotes | One-to-Many |
| PollOption → PollVotes | One-to-Many |

### Important Constraints

| Constraint | Purpose |
|---|---|
| `users.email` unique | Prevent duplicate presenter accounts |
| `sessions.access_code` unique | Ensure each join code maps to one session |
| `participants.participant_token_hash` unique | Store participant token hashes uniquely |
| `question_votes(question_id, participant_id)` unique | Prevent duplicate question upvotes |
| `poll_votes(poll_id, participant_id)` unique | Prevent duplicate poll votes |

### Status Enums

| Enum | Values |
|---|---|
| `SessionStatus` | `DRAFT`, `SCHEDULED`, `LIVE`, `ENDED`, `ARCHIVED` |
| `QuestionStatus` | `PENDING`, `APPROVED`, `HIDDEN`, `ANSWERED` |
| `PollStatus` | `DRAFT`, `PUBLISHED`, `CLOSED` |
| `PollType` | `SINGLE_CHOICE`, `MULTIPLE_CHOICE` |
| `TokenType` | `ACCESS`, `REFRESH`, `PARTICIPANT` |
| `UserRole` | `PRESENTER`, `ADMIN` |

---

## ✅ Validation and Business Rules

### Authentication Rules

- Passwords must never be stored or returned in plain text
- Register requires name, valid email, and password with minimum length 8
- Login normalizes email by trimming and lowercasing
- Disabled users cannot log in
- Refresh tokens must exist, be valid, not expired, and not revoked
- Refresh token flow revokes the old token and creates a new token pair
- WebSocket accepts only access tokens and participant tokens
- WebSocket rejects refresh tokens

### Session Rules

- Only authenticated presenters can create sessions
- Only the session owner can update, start, end, archive, or delete a session
- New sessions are created as `SCHEDULED`
- Only `SCHEDULED` sessions can be started
- Only `LIVE` sessions can be ended
- Live sessions cannot be archived
- Archived sessions cannot be archived again
- Only `SCHEDULED` sessions can be deleted
- Ended or archived sessions cannot be edited

### Participant Rules

- Participants can join only `LIVE` sessions
- Access code is normalized by trimming and uppercasing
- Display name is required when `anonymousAllowed` is false
- Missing display name becomes `Anonymous` when anonymous access is allowed
- Participant REST actions require a valid participant token
- Participants can act only inside their own session
- Muted participants cannot submit questions, delete questions, upvote, or vote on polls

### Question Rules

- Questions can be submitted only to live sessions
- Questions are limited to 1000 characters
- Moderation-enabled sessions create questions as `PENDING`
- Moderation-disabled sessions create questions as `APPROVED`
- Only session owners can approve, hide, pin, unpin, or answer questions
- Only pending questions can be approved or hidden
- Only approved questions can be pinned, unpinned, answered, or upvoted
- Marking a question as answered changes status to `ANSWERED`, sets `answeredAt`, and unpins it
- Participants can delete only their own questions
- A participant can upvote the same question once

### Poll Rules

- Poll title must be 1 to 200 characters
- Poll must contain 2 to 10 options
- Each poll option must be 1 to 200 characters
- Draft polls can be created only for `LIVE` or `SCHEDULED` sessions
- Polls can be published only during `LIVE` sessions
- Only `DRAFT` polls can be published
- Only `PUBLISHED` polls can be voted on
- Only `PUBLISHED` polls can be closed
- Participants can vote only in their own session
- Participants can vote once per poll in the current implementation
- Only draft polls can be deleted

---

## 🛡️ Error Handling

Errors use a consistent response model.

### Example Resource Error

```json
{
  "timestamp": "2026-05-07T14:20:00",
  "status": 404,
  "error": "RESOURCE_NOT_FOUND",
  "message": "Session not found",
  "path": "/api/v1/sessions/10",
  "fields": null
}
```

### Example Validation Error

```json
{
  "timestamp": "2026-05-07T14:20:00",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Validation failed",
  "path": "/api/v1/sessions",
  "fields": {
    "title": "must not be blank"
  }
}
```

### Error Codes

| Error Code | Meaning |
|---|---|
| `EMAIL_ALREADY_EXISTS` | Registration email already exists |
| `INVALID_CREDENTIALS` | Login credentials are wrong |
| `INVALID_REFRESH_TOKEN` | Refresh token is missing, expired, revoked, or invalid |
| `UNAUTHORIZED` | Authentication is missing or invalid |
| `FORBIDDEN` | Authenticated actor is not allowed to perform action |
| `RESOURCE_NOT_FOUND` | Requested resource does not exist or is hidden by ownership rules |
| `VALIDATION_ERROR` | Request body or business validation failed |
| `INVALID_SESSION_STATUS` | Session state does not allow the action |
| `INVALID_QUESTION_STATUS` | Question state does not allow the action |
| `INVALID_POLL_STATUS` | Poll state does not allow the action |
| `DUPLICATE_RESOURCE` | Unique constraint or duplicate action conflict |
| `INTERNAL_SERVER_ERROR` | Unexpected server error |

---

## 🧪 Testing

The Maven configuration includes testing dependencies for:

- Spring Boot tests
- Spring Security tests
- JUnit 5
- Testcontainers
- PostgreSQL Testcontainers
- RabbitMQ Testcontainers

Run tests with:

```bash
./mvnw test
```

On Windows:

```bash
mvnw.cmd test
```

### Current Testing Status

No dedicated test classes were included in the uploaded project snapshot. The project is ready to add tests, but this README does not claim that a full test suite already exists.

### Recommended Test Coverage

| Test Area | What to Verify |
|---|---|
| Auth service | Register, login, duplicate email, disabled user, refresh rotation |
| Session service | Owner-only access, status transitions, access code uniqueness |
| Participant service | Join live session, reject invalid code, reject non-live session |
| Question service | Pending/public flow, owner moderation, participant ownership delete |
| Upvotes | Duplicate prevention, approved-only voting, muted participant rejection |
| Poll service | Draft/publish/close flow, duplicate votes, published-only voting |
| WebSocket auth | Valid token connects, refresh token rejected, participant token accepted |
| Integration tests | PostgreSQL constraints, transactions, repository behavior |

---

## 🚀 How to Run Locally

### Prerequisites

- Java 21+
- Maven or Maven Wrapper
- PostgreSQL 14+
- Git

### 1. Clone the Repository

```bash
git clone https://github.com/your-username/echoboard.git
cd echoboard
```

### 2. Create PostgreSQL Database

```sql
CREATE DATABASE echoboard;
```

### 3. Create Environment File

Copy the example environment file:

```bash
cp .env.example .env
```

Update values for your local PostgreSQL instance:

```properties
DB_HOST=localhost
DB_PORT=5432
DB_NAME=echoboard
DB_USERNAME=postgres
DB_PASSWORD=your_password_here

JWT_SECRET=replace-with-a-long-secret-key-at-least-32-characters
JWT_ACCESS_TOKEN_EXPIRATION_MS=900000
JWT_REFRESH_TOKEN_EXPIRATION_MS=604800000
JWT_PARTICIPANT_TOKEN_EXPIRATION_MS=43200000
```

### 4. Run the Application

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

### 5. Open Swagger

```text
http://localhost:8080/swagger-ui.html
```

### 6. Check Application Health

```text
http://localhost:8080/actuator/health
```

---

## ⚙️ Configuration

The root application configuration imports environment variables from `.env` and activates the `dev` profile:

```properties
spring.config.import=optional:file:.env[.properties]
spring.profiles.active=dev
```

The development profile uses PostgreSQL and JWT settings from environment variables:

```properties
spring.application.name=EchoBoard
server.port=8080

spring.datasource.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false

management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always

springdoc.swagger-ui.path=/swagger-ui.html

app.jwt.secret=${JWT_SECRET}
app.jwt.access-token-expiration-ms=${JWT_ACCESS_TOKEN_EXPIRATION_MS}
app.jwt.refresh-token-expiration-ms=${JWT_REFRESH_TOKEN_EXPIRATION_MS}
app.jwt.participant-token-expiration-ms=${JWT_PARTICIPANT_TOKEN_EXPIRATION_MS}
```

### Required Environment Variables

| Variable | Description |
|---|---|
| `DB_HOST` | PostgreSQL host |
| `DB_PORT` | PostgreSQL port |
| `DB_NAME` | PostgreSQL database name |
| `DB_USERNAME` | PostgreSQL username |
| `DB_PASSWORD` | PostgreSQL password |
| `JWT_SECRET` | Secret used to sign JWT tokens |
| `JWT_ACCESS_TOKEN_EXPIRATION_MS` | Access token lifetime in milliseconds |
| `JWT_REFRESH_TOKEN_EXPIRATION_MS` | Refresh token lifetime in milliseconds |
| `JWT_PARTICIPANT_TOKEN_EXPIRATION_MS` | Participant token lifetime in milliseconds |

> Never commit real database passwords, JWT secrets, access tokens, refresh tokens, participant tokens, or production credentials.

---

## 🗄️ Database Setup Notes

The development configuration uses:

```properties
spring.jpa.hibernate.ddl-auto=update
```

This is convenient during development because Hibernate can update tables automatically. For production, a migration tool such as **Flyway** or **Liquibase** should be introduced so database changes are versioned and reviewable.

### Development Database Creation

```sql
CREATE DATABASE echoboard;
```

### Expected Tables

When the application starts successfully, Hibernate should create/update tables similar to:

```text
users
refresh_tokens
sessions
participants
questions
question_votes
polls
poll_options
poll_votes
```

---

## 🎯 Key Design Decisions

| Decision | Reason |
|---|---|
| REST for commands | Keeps validation, transactions, status codes, and API documentation simple |
| WebSocket for broadcasts | Pushes real-time updates to clients without polling |
| Service-layer authorization | Ensures business rules are enforced even if controllers change |
| Participant token model | Allows guest users to act inside one session without becoming full accounts |
| DTO-based API | Prevents exposing JPA entities and keeps responses stable |
| MapStruct mapping | Reduces boilerplate while keeping mapping type-safe |
| PostgreSQL unique constraints | Adds a durable safety net for duplicate votes |
| Refresh token persistence | Supports logout/revocation and refresh token rotation |
| In-memory presence first | Simple MVP implementation before Redis-backed distributed presence |
| Redis planned later | Useful for rate limiting, distributed presence, cached session codes, and high-frequency counters |
| RabbitMQ planned later | Useful for async analytics, notifications, retries, and dead-letter queues |
| Actuator included early | Provides a foundation for health checks and monitoring |

---

## 🖼️ Screenshots and Demo Placeholders

No screenshots or demo GIFs were included in the current snapshot. Add them later under a `docs/` directory.

| Asset | Suggested Path | Status |
|---|---|---|
| Swagger UI screenshot | `docs/screenshots/swagger-ui.png` | TODO |
| Presenter session dashboard | `docs/screenshots/presenter-dashboard.png` | TODO |
| Participant join screen | `docs/screenshots/participant-join.png` | TODO |
| Live question moderation demo | `docs/gifs/question-moderation.gif` | TODO |
| Live poll voting demo | `docs/gifs/live-polls.gif` | TODO |
| WebSocket presence demo | `docs/gifs/presence.gif` | TODO |

---

## 🔮 Roadmap

### Redis Roadmap

- Rate limit question submissions and votes
- Rate limit login attempts
- Store distributed presence with TTL
- Cache session access-code lookups
- Maintain high-frequency poll counters in Redis
- Periodically sync counters back to PostgreSQL

### RabbitMQ Roadmap

- Publish async event when a session is created
- Publish async event when a session ends
- Generate session analytics in the background
- Add mock email/log notifications
- Add retry policies
- Add dead-letter queues for failed messages

### Analytics and Export Roadmap

- Session analytics endpoint
- Question summary
- Poll result summary
- Participant engagement metrics
- CSV export for questions and poll results

### Production Readiness Roadmap

- Dockerfile
- Docker Compose for app + PostgreSQL + Redis + RabbitMQ
- GitHub Actions CI pipeline
- Integration tests with Testcontainers
- WebSocket integration tests
- Flyway or Liquibase migrations
- Structured JSON logging
- Better observability with metrics
- Production profile with safer JPA settings
- API versioning policy

---

## 📚 What I Learned

- Designing a real-time backend with REST commands and WebSocket broadcasts
- Building JWT authentication with Spring Security
- Implementing refresh token persistence, rotation, and revocation
- Creating participant-scoped JWT tokens for guest users
- Protecting owner-only resources at the service layer
- Modeling session, question, and poll lifecycles with explicit statuses
- Preventing duplicate votes with both service checks and database constraints
- Using WebSocket/STOMP topics for real-time updates
- Handling WebSocket authentication through a channel interceptor
- Building DTO-based API contracts with MapStruct
- Creating consistent error responses with `@RestControllerAdvice`
- Using PostgreSQL relationships and constraints for domain integrity
- Preparing a project roadmap for Redis, RabbitMQ, Docker, CI, and Testcontainers

---

## 📝 Notes / Current Status

- The current implementation uses REST endpoints for question and poll commands, then broadcasts updates through WebSocket topics.
- Presence is currently stored in memory using concurrent maps, so it is suitable for a single application instance only.
- Redis-backed distributed presence is planned but not implemented yet.
- RabbitMQ async processing is planned but not implemented yet.
- Poll `MULTIPLE_CHOICE` exists as an enum, but the current database constraint allows only one vote per participant per poll.
- A separate moderator entity/assignment workflow is not implemented yet; moderation is currently owner-only.
- `SessionStatus.DRAFT` exists as an enum value, but new sessions are currently created as `SCHEDULED`.
- Poll `DELETED` exists as an event enum, but draft poll deletion currently does not broadcast a public WebSocket delete event.
- Docker Compose, CI, analytics, CSV export, scheduled jobs, and production deployment are upcoming improvements.

---

## 👤 Author

**Mohamed Abdul Shafi**

📧 **Email:** [mohamedsadik763@gmail.com](mailto:mohamedsadik763@gmail.com)

---

<div align="center">

Built with dedication and a focus on clean backend architecture, real-time systems, and production-style engineering practices.

⭐ If you found this project useful, consider giving it a star.

</div>
