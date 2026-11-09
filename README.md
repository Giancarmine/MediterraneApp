# MediterraneApp

[![CI](https://github.com/Giancarmine/MediterraneApp/actions/workflows/ci.yml/badge.svg)](https://github.com/Giancarmine/MediterraneApp/actions/workflows/ci.yml)

A small REST API for recipes, built with Spring Boot. It stores recipes, their
ingredients, and their preparation steps in PostgreSQL.

> **Status:** legacy portfolio project from 2020. The code builds and has an automated
> test suite plus CI, but no authentication, and the checked-in database configuration
> points at a retired instance. Read [`docs/KNOWN_ISSUES.md`](docs/KNOWN_ISSUES.md)
> before deploying or extending it.

## Stack

- Java 17
- Spring Boot 4.1.2 (Spring MVC, Spring Data JPA, Hibernate 7)
- PostgreSQL
- Lombok (version managed by the Spring Boot BOM)
- Jasypt Spring Boot 4.0.4 for encrypted configuration values
- Maven 3.9.16 via the committed wrapper (`./mvnw`)

## API

All endpoints live under `/recipe`.

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/recipe/status` | Returns `"I_AM_A_TEAPOT"` as a JSON string. |
| `GET` | `/recipe` | Lists recipes. |
| `GET` | `/recipe/{uuid}` | Reads one recipe. |
| `POST` | `/recipe` | Creates a recipe, or updates one with the same name. |
| `POST` | `/recipe/bulk` | Creates or updates a list of recipes. |
| `PUT` | `/recipe/{uuid}` | Updates a recipe. |
| `DELETE` | `/recipe/{uuid}` | Deletes a recipe. |

Full payloads, status codes, and quirks are in [`docs/API.md`](docs/API.md). There is no
ingredient endpoint yet.

Example:

```bash
curl -s http://localhost:8080/recipe/status
```

## Run locally

You need JDK 17 and PostgreSQL. The committed wrapper downloads Maven 3.9.16.

```bash
docker run --rm --name mediterraneapp-db \
  -e POSTGRES_DB=mediterraneapp \
  -e POSTGRES_USER=mediterraneapp \
  -e POSTGRES_PASSWORD=change-me \
  -p 5432:5432 -d postgres:16

export SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/mediterraneapp'
export SPRING_DATASOURCE_USERNAME='mediterraneapp'
export SPRING_DATASOURCE_PASSWORD='change-me'

./mvnw -B clean package
./mvnw spring-boot:run
```

The API starts on <http://localhost:8080>. Hibernate creates the schema on first run
because `spring.jpa.hibernate.ddl-auto` is `update`.

Build only:

```bash
./mvnw -B -DskipTests package
# target/mediterraneapp-0.0.1-SNAPSHOT.jar
```

Tests:

```bash
./mvnw test
```

The suite runs on an in-memory H2 database in PostgreSQL mode, so it needs no
PostgreSQL server and never touches the checked-in remote database. It covers the
mappers, the repository, and the full HTTP API (including the documented 500
responses). CI runs `./mvnw -B verify` on every push and pull request to `main`.

## Configuration

Spring Boot relaxed binding reads the datasource from the environment:

| Variable | Purpose |
| --- | --- |
| `SPRING_DATASOURCE_URL` | JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | Database user |
| `SPRING_DATASOURCE_PASSWORD` | Database password |
| `JASYPT_ENCRYPTOR_PASSWORD` | Key for `ENC(...)` values, if used |
| `SERVER_PORT` | HTTP port, default `8080` |

See [`.env.example`](.env.example). Spring Boot does not load `.env` automatically;
export the variables or pass the file to Docker.

The values committed in `src/main/resources/application.yml` are historical and must not
be reused. See [`SECURITY.md`](SECURITY.md).

## Docker

```bash
docker build -t mediterraneapp .
docker run --rm -p 8080:8080 \
  -e SPRING_DATASOURCE_URL='jdbc:postgresql://host.docker.internal:5432/mediterraneapp' \
  -e SPRING_DATASOURCE_USERNAME='mediterraneapp' \
  -e SPRING_DATASOURCE_PASSWORD='change-me' \
  mediterraneapp
```

The Dockerfile uses `maven:3.9-eclipse-temurin-17` to build and
`eclipse-temurin:17-jre` to run.

## Documentation

- [`AGENTS.md`](AGENTS.md) — canonical guide for AI coding agents.
- [`docs/API.md`](docs/API.md) — endpoint reference.
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — layers, request flow, data model.
- [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) — local, Docker, and legacy Heroku notes.
- [`docs/KNOWN_ISSUES.md`](docs/KNOWN_ISSUES.md) — prioritized defects and fixes.
- [`CONTRIBUTING.md`](CONTRIBUTING.md) — how to contribute.
- [`SECURITY.md`](SECURITY.md) — how to report a vulnerability.

## License

GPL-3.0. See [`LICENSE`](LICENSE).
