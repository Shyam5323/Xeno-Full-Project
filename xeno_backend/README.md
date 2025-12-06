#  Shopify Multi‑Tenant Ingestion & Insights

Spring Boot service that onboards multiple Shopify stores (tenants), ingests Customers/Products/Orders, and serves analytics to a Next.js dashboard.

## Features

- JWT authentication (register/login)
- Tenant onboarding with encrypted Shopify Admin API token at rest
- Full and incremental data sync (cursor pagination) for:
	- Customers (+ addresses)
	- Products & variants (stock)
	- Orders & line items
- Scheduler + RabbitMQ per‑tenant queues + DLX/DLQ
- Redis caching for hot analytics
- Row‑level multi‑tenancy with access enforcement
- Optional failure notifications via SMTP

## Tech stack

- Spring Boot 3.3 (Java 21), Spring Security, Spring Data JPA
- MySQL (production), HikariCP
- Redis cache (optional)
- RabbitMQ (optional for async ingestion; can be disabled)
- JWT (jjwt), BCrypt
- Lombok, Validation (Jakarta)

## Quick start

Prerequisites:
- Java 21
- MySQL 8+
- Optional: Redis, RabbitMQ, SMTP

Environment variables (examples shown as .env — spring‑dotenv is included so .env will be loaded):

```
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/xeno?useSSL=false&serverTimezone=UTC
MYSQLUSER=root
MYSQLPASSWORD=your_password
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect

# JWT & token crypto
JWT_SECRET=replace-with-strong-secret
TOKEN_CRYPTO_KEY=replace-with-32b-key

# Redis (optional)
spring.cache.type=redis
REDISHOST=localhost
REDISPORT=6379
REDIS_PASSWORD=

# RabbitMQ (optional)
RABBITMQ_URL=amqps://user:pass@host/vhost
sync.messaging.enabled=true

# Mail (optional)
MAIL_USERNAME=you@example.com
MAIL_PASSWORD=app-password
```

Local schema:
- Default config uses `spring.jpa.hibernate.ddl-auto=none` to avoid accidental schema changes.
- For local only, you can set: `SPRING_JPA_HIBERNATE_DDL_AUTO=update` (env var) to auto‑create/update tables.

Run (Windows PowerShell):

```
./gradlew.bat bootRun
```

Run (macOS/Linux):

```
./gradlew bootRun
```

Base URL: `http://localhost:8080/api`

Build a jar:

```
./gradlew.bat clean build
```

## API summary

Authentication
- POST `/api/auth/register`
- POST `/api/auth/login`

Tenant onboarding & access (JWT)
- POST `/api/tenant-access/onboard` { shopDomain, accessToken }
- GET `/api/tenant-access/my-tenants`
- GET `/api/tenant-access/tenant/{tenantId}`
- PUT `/api/tenant-access/tenant/{tenantId}/role` { role }
- DELETE `/api/tenant-access/tenant/{tenantId}`
- GET `/api/tenant-access/tenant/{tenantId}/users`
- GET `/api/tenant-access/tenant/{tenantId}/stats`
- GET `/api/tenant-access/my-tenant-stats`

Tenant lifecycle (admin)
- POST `/api/tenants/onboard` { shopDomain, accessToken }
- GET `/api/tenants/{tenantId}`
- DELETE `/api/tenants/{tenantId}`

Sync (JWT)
- POST `/api/sync/full`           header: `X-Tenant-ID`
- POST `/api/sync/incremental?since=ISO` header: `X-Tenant-ID`
- POST `/api/sync/full/all`
- POST `/api/sync/incremental/all?since=ISO`

Sync jobs (RabbitMQ)
- POST `/api/sync/jobs` body: { type: FULL|INCREMENTAL, tenantId: ALL|<id>, since? }
- POST `/api/sync/jobs/single/{tenantId}?type=…&since=…`

Analytics (JWT; tenant‑scoped)
- GET `/api/analytics/revenue?start=ISO&end=ISO`
- GET `/api/analytics/revenue/daily?start=YYYY-MM-DD&end=YYYY-MM-DD`
- GET `/api/analytics/orders/status-breakdown`
- GET `/api/analytics/customers/top?limit=N`
- GET `/api/analytics/customers/stockout?limit=N`
- GET `/api/analytics/aov?start=ISO&end=ISO`
- GET `/api/analytics/upt?start=ISO&end=ISO`
- GET `/api/analytics/products/top?by=revenue|quantity&limit=N&start=ISO&end=ISO`
- GET `/api/analytics/customers/new-vs-returning?start=ISO&end=ISO`
- GET `/api/analytics/orders/cancellation-rate?start=ISO&end=ISO`

Events (JWT; tenant‑scoped)
- POST `/api/events` header `X-Tenant-ID`, body: { customerId?, eventType, data?, sessionId?, ip?, userAgent? }

Headers
- Authorization: `Bearer <JWT>` (most endpoints)
- X-Tenant-ID: `<tenantId>` (analytics/events/sync per‑tenant)

## Multi‑tenancy

- Row‑level isolation: all domain tables carry `tenant_id`.
- `MultiTenantFilter` sets tenant context from headers `X-Tenant-ID` (or `X-Shop-Domain`), exposed via `TenantContext`.
- Controllers validate user access to a tenant via `UserTenantAccessService`.

## Shopify integration & sync

- `ShopifyApiService` (API version 2024‑07) implements cursor pagination (`page_info` in Link header), retries and simple backoff.
- `SyncService` orchestrates full and incremental syncs across Customers, Products/Variants, and Orders.
- `DataSyncService` runs sync across all active tenants.
- Scheduler: `SyncJobScheduler` enqueues periodic jobs per tenant.
- Messaging: `TenantQueueProvider` declares per‑tenant queues with DLX/DLQ, `SyncJobListener` consumes and executes.
- Observability: `SyncLog` records segment status, counts, and errors; optional email on failures.

Disable messaging locally:
- Set `sync.messaging.enabled=false` to avoid RabbitMQ dependency.

Disable Redis locally:
- Set `spring.cache.type=none`.

## Running tests

```
./gradlew.bat test
```

## Deployment notes

- Railway/Render recommended for backend (managed MySQL, RabbitMQ). Set env vars accordingly.
- Health endpoints via Spring Actuator are included (dependency), wire up exposure as needed.

## Troubleshooting

- 401 Unauthorized: missing/invalid JWT ⇒ login and send `Authorization: Bearer <token>`
- 403 Forbidden: user lacks access to `X-Tenant-ID` ⇒ check `/api/tenant-access/my-tenants`
- DB errors on startup: ensure schema exists; for local dev set `SPRING_JPA_HIBERNATE_DDL_AUTO=update`
- Redis connection issues: disable cache via `spring.cache.type=none`
- RabbitMQ connection issues: disable via `sync.messaging.enabled=false` or configure `RABBITMQ_URL`

---

See the root `documentation.md` for a complete architecture overview and next steps to production.

