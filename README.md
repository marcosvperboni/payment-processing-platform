# Payment Processing Platform

A simulated payment acquirer/processor built as a portfolio project. It receives a payment request (credit card, PIX or boleto), runs it through an asynchronous authorization pipeline, and exposes capture, cancellation, refund and webhook-delivery flows — the same shape of problem found in real payment gateways.

## Payment lifecycle

```
CREATED -> PROCESSING -> APPROVED -> SETTLED -> REFUNDED
                       \-> DECLINED
   \-------------------------------> CANCELLED (before settlement)
```

All transitions are enforced by the `Payment` aggregate itself (`domain/model/Payment.java`); an invalid transition (e.g. capturing a payment that was never approved) is rejected with `409 Conflict` before it ever reaches the database.

## Architecture

- **Domain-driven design**: `Payment` is a rich aggregate that owns its state machine; application services (`PaymentService`, `WebhookEndpointService`) orchestrate use cases and stay free of business rules.
- **Asynchronous authorization** via **Kafka**: creating a payment publishes a `payment.authorization.requested` event; a simulated gateway (`PaymentGatewaySimulator`) consumes it, waits a configurable delay, and approves or declines the payment. Failed message processing is retried and then routed to a `.DLT` dead-letter topic instead of blocking the partition.
- **Webhook delivery** via **RabbitMQ**: every status change is queued for delivery to every registered webhook endpoint. Delivery failures are retried up to 3 times; if still failing, the message is dead-lettered into a DLQ and recorded in the payment's own history as a `WEBHOOK_FAILED` operation.
- **Idempotency**: every payment creation call requires an `Idempotency-Key` header. The key is cached in **Redis** for a fast duplicate check, backed by a unique constraint on the same key in PostgreSQL as the source of truth.
- **Audit trail**: every operation performed on a payment (created, authorization requested, approved/declined, captured, cancelled, refunded, webhook delivered/failed) is appended to a `payment_operations` table, exposed via `GET /api/v1/payments/{id}/history`.
- **Security**: stateless JWT authentication (`Spring Security` + `jjwt`). Two demo accounts are seeded on first boot: `admin/admin123` and `merchant/merchant123`.

## Tech stack

| Concern | Technology |
|---|---|
| Language / runtime | Java 25 |
| Framework | Spring Boot 4 (Web, Security, Data JPA, Validation, Actuator) |
| Database | PostgreSQL, versioned with Flyway |
| Cache / idempotency store | Redis |
| Async messaging | Apache Kafka (authorization pipeline) |
| Queueing / DLQ | RabbitMQ (webhook delivery) |
| Auth | JWT (jjwt) |
| API docs | springdoc-openapi / Swagger UI |
| Tests | JUnit 5, Mockito, AssertJ, Testcontainers |
| Local infra | Podman / podman-compose |
| CI | GitHub Actions |

## Running locally

1. Start the infrastructure:
   ```
   podman-compose -f podman-compose.yml up -d
   ```
2. Run the application:
   ```
   ./mvnw spring-boot:run
   ```
3. Open Swagger UI at `http://localhost:18095/swagger-ui.html`.

Default ports were chosen off the beaten path (`18095` for the app, `15490`/`16390`/`19095`/`15690` for Postgres/Redis/Kafka/RabbitMQ) to avoid clashing with other services that might already be running locally.

## Authenticating

```
POST /api/v1/auth/login
{ "username": "merchant", "password": "merchant123" }
```

Use the returned token as `Authorization: Bearer <token>` on every other endpoint.

## Core endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/payments` | Create a payment (requires `Idempotency-Key` header) |
| GET | `/api/v1/payments/{id}` | Fetch a payment |
| GET | `/api/v1/payments?status=` | List payments, optionally filtered by status |
| GET | `/api/v1/payments/{id}/history` | Full operation history |
| POST | `/api/v1/payments/{id}/capture` | Capture an approved payment |
| POST | `/api/v1/payments/{id}/refund` | Refund a settled payment |
| DELETE | `/api/v1/payments/{id}` | Cancel a payment (before settlement) |
| POST / GET / PUT / DELETE | `/api/v1/webhook-endpoints` | Manage merchant webhook URLs |

Every request body is validated with Bean Validation; invalid input, unknown resources, and illegal state transitions map to `400`, `404` and `409` respectively through a single `GlobalExceptionHandler`.

## Testing

```
./mvnw test
```

The suite includes pure domain unit tests for the payment state machine, Mockito-based service tests, MockMvc slice tests for request validation on every controller, and a full end-to-end integration test (`PaymentFlowIntegrationTest`) that boots the real application against Testcontainers-managed Postgres, Redis, Kafka and RabbitMQ — covering the happy path, the decline path, idempotent replay, authentication, webhook CRUD, and the webhook dead-letter path.
