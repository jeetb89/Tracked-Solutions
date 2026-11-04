# Order Management Service

A Spring Boot REST API for creating, listing, updating, and deleting customer orders. It uses Spring Data JPA with PostgreSQL, Redis caching, and Kafka order events.

## Requirements

- Java 17 or newer
- Maven 3.6.3 or newer
- PostgreSQL 16 or newer (available through the repository's Docker Compose setup)
- Redis 7 or newer (available through the repository's Docker Compose setup)
- Apache Kafka 4.1 or newer (available through the repository's Docker Compose setup)

## Run

From this directory:

```sh
mvn spring-boot:run
```

Start PostgreSQL, Redis, and Kafka from the repository root with `docker compose up -d postgres redis kafka`. The service uses Redis as a cache for order lookups, with a 10-minute TTL. PostgreSQL remains the source of truth.

The API listens at `http://localhost:8080`. PostgreSQL connection settings can be supplied with `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`; defaults match the repository's Compose configuration.

## Redis caching

Redis is used as a shared cache across service instances. The service uses Spring Cache annotations (`@Cacheable` and `@CacheEvict`) for individual order lookups by database ID and order number. Customer-email order lists use `RedisTemplate` directly with keys under `orders:customer:` and the same TTL configured by `spring.cache.redis.time-to-live` (10 minutes by default).

Create, update, and delete operations evict the affected customer-list keys after the database transaction commits. They also clear the annotated order lookup caches. The `RedisTemplate` uses string keys and Java serialization for cached response values; the response DTOs are serializable.

## Kafka order events

The service writes an outbox row in the same database transaction as each order create, update, and delete. A scheduled publisher sends pending rows as JSON events to `order-events` and marks them published only after Kafka acknowledges them. This avoids losing events when the broker is temporarily unavailable; a crash after Kafka accepts a record but before the outbox update can cause a duplicate, so consumers should be idempotent. The `OrderEventListener` consumes events with the `order-service` group. Listener failures are retried with exponential backoff, then routed to `order-events-dlt` with the original payload and error headers.

The Kafka broker address defaults to `localhost:9092` and can be changed with `KAFKA_BOOTSTRAP_SERVERS`. The local Compose broker uses a single partition and is intended for development.

## Endpoints

| Method | Path | Action |
| --- | --- | --- |
| `POST` | `/api/v1/orders` | Create an order |
| `GET` | `/api/v1/orders` | List orders, optionally filtered by `customerEmail` |
| `GET` | `/api/v1/orders/{orderNumber}` | Get one order by its business number |
| `PUT` | `/api/v1/orders/{orderNumber}` | Replace an order's editable fields |
| `DELETE` | `/api/v1/orders/{orderNumber}` | Delete an order |

Order JSON:

```json
{
  "customerEmail": "avery@example.com",
  "status": "PENDING",
  "items": [
    { "productCode": "KB-001", "quantity": 1, "unitPrice": 129.95 }
  ]
}
```

The service generates a UUID `orderNumber`, records `createdAt`, and calculates `totalAmount` from item quantities and unit prices. Allowed statuses are `PENDING`, `CONFIRMED`, and `CANCELLED`; omitted status defaults to `PENDING`. Each order item stores a product code, quantity, and unit price.

The create and update endpoints accept validated request DTOs. All read and write endpoints return an `OrderResponse` DTO containing the order number, customer email, status, total amount, creation time, and line items; JPA entities are not used as API payloads.

```sh
curl -X POST http://localhost:8080/api/v1/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerEmail":"avery@example.com","status":"PENDING","items":[{"productCode":"KB-001","quantity":1,"unitPrice":129.95}]}'

curl http://localhost:8080/api/v1/orders
curl 'http://localhost:8080/api/v1/orders?customerEmail=avery@example.com'
curl http://localhost:8080/api/v1/orders/ORDER_NUMBER \
  -H 'Accept: application/json'
curl -X PUT http://localhost:8080/api/v1/orders/ORDER_NUMBER \
  -H 'Content-Type: application/json' \
  -d '{"customerEmail":"avery@example.com","status":"CONFIRMED","items":[{"productCode":"KB-001","quantity":1,"unitPrice":149.95}]}'
curl -i -X DELETE http://localhost:8080/api/v1/orders/ORDER_NUMBER
```

Customer email is required and must be valid. Each item requires a product code, positive quantity, and positive unit price. Missing order IDs or order numbers return `404 Not Found`.
