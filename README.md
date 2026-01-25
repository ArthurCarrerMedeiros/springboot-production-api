# springboot-production-api

Spring Boot REST API built with production patterns: Postgres, Flyway, JWT, Actuator, tests, Docker, CI.

## Quick Start

### Prerequisites

- Java 21
- Docker and Docker Compose
- Maven (or use the included Maven wrapper)

### Running Locally

1. Start the database:

```bash
docker compose up -d
```

2. Run the application:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The API will be available at `http://localhost:8080`.

### Stopping the Database

```bash
docker compose down
```

To remove the data volume as well:

```bash
docker compose down -v
```
