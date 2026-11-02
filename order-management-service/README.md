# Order Management Service

A Spring Boot REST API for creating, listing, updating, and deleting customer orders. It uses Spring Data JPA and an in-memory H2 database; data resets when the application stops.

## Requirements

- Java 17 or newer
- Maven 3.6.3 or newer
- Redis 7 or newer (available through the repository's Docker Compose setup)

## Run

From this directory:

```sh
mvn spring-boot:run
```

Start Redis first from the repository root with `docker compose up -d redis`. The service uses Redis as a cache for order lookups by ID and order number, with a 10-minute TTL. Creates, updates, and deletes clear both order lookup caches. PostgreSQL remains the source of truth.

The API listens at `http://localhost:8080`. The H2 console is available at `http://localhost:8080/h2-console` with JDBC URL `jdbc:h2:mem:orders`, username `sa`, and a blank password.

## Redis caching

Redis is used as a shared cache across service instances. The service uses Spring Cache annotations (`@Cacheable` and `@CacheEvict`) for individual order lookups by database ID and order number. Customer-email order lists use `RedisTemplate` directly with keys under `orders:customer:` and the same TTL configured by `spring.cache.redis.time-to-live` (10 minutes by default).

Create, update, and delete operations evict the affected customer-list keys after the database transaction commits. They also clear the annotated order lookup caches. The `RedisTemplate` uses string keys and Java serialization for cached response values; the response DTOs are serializable.

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
