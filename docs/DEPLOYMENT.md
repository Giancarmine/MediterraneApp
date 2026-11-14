# Deployment and local setup

The project is a standard Spring Boot JAR. It can run locally with Maven, in Docker, or
on a Java platform such as Heroku. No database credentials are checked in, so supply the
datasource through the environment; the paths below are templates.

## Prerequisites

- JDK 25 LTS (the POM targets Java 25).
- Maven is not required separately: the committed wrapper downloads Maven 3.9.16.
- PostgreSQL for a real run (12 or newer recommended). Flyway creates the schema.
- Docker is optional.

## Local run

1. Start PostgreSQL:

   ```bash
   docker run --rm --name mediterraneapp-db \
     -e POSTGRES_DB=mediterraneapp \
     -e POSTGRES_USER=mediterraneapp \
     -e POSTGRES_PASSWORD=change-me \
     -p 5432:5432 -d postgres:16
   ```

2. Export the configuration overrides:

   ```bash
   export SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/mediterraneapp'
   export SPRING_DATASOURCE_USERNAME='mediterraneapp'
   export SPRING_DATASOURCE_PASSWORD='change-me'
   export APP_API_KEY='change-me-to-a-long-random-value'
   ```

3. Build and run:

   ```bash
   ./mvnw -B clean package
   ./mvnw spring-boot:run
   ```

4. Verify:

   ```bash
   curl -i http://localhost:8080/recipe/status
   # 200, body: "I_AM_A_TEAPOT"
   curl -i 'http://localhost:8080/recipe?page=0&size=20' \
     -H "X-API-Key: $APP_API_KEY"
   # 200, body: {"content":[],"page":0,"size":20,"totalElements":0,"totalPages":0}
   curl -i 'http://localhost:8080/recipe?page=0&size=20'
   # 401 without the header
   ```

On the first startup Flyway applies `db/migration/V1__baseline.sql`, then Hibernate
validates the schema with `spring.jpa.hibernate.ddl-auto=validate`. There is one baseline
migration; add `V2__*.sql` (and so on) for later schema changes. Never edit an applied
migration: Flyway checksums it and startup fails if the file changes.

Existing databases previously managed by `ddl-auto: update` are baselined at version 1 by
`spring.flyway.baseline-on-migrate`, so the baseline script is not re-run. They must
already contain the current schema, including `uk_recipe_name` and
`uk_ingredient_identity`. Clean up duplicate names or ingredient triples before upgrading;
otherwise the constraints may be missing or the migration/validation fails.

### Using `.env`

`.env.example` documents the variable names, but Spring Boot does not read `.env`
automatically. Either export the values, use a tool such as `direnv`, or pass the file
to Docker with `--env-file`. Never commit a filled `.env`.

## Configuration reference

| Variable | Default in `application.yml` | Required |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | none | Yes |
| `SPRING_DATASOURCE_USERNAME` | none | Yes |
| `SPRING_DATASOURCE_PASSWORD` | none | Yes |
| `SERVER_PORT` | `8080` | No |
| `APP_API_KEY` | none | Yes; startup fails when blank |
| `APP_CORS_ALLOWED_ORIGINS` | empty (CORS disabled) | No |
| `JAVA_OPTS` | none | No, used by the Docker `CMD` |

Spring Boot relaxed binding maps `SPRING_DATASOURCE_*` to `spring.datasource.*`.
Environment variables take precedence over `application.yml`.

Other current settings:

- Hikari: `connectionTimeout=20000`, `maximumPoolSize=5`.
- `spring.jpa.show-sql=true`.
- `spring.jpa.hibernate.ddl-auto=validate`; Flyway applies migrations from
  `src/main/resources/db/migration` and baselines existing schemas at V1.
- Unique constraints: `uk_recipe_name` and `uk_ingredient_identity`. Deduplicate existing
  data before deploying this version.
- Hibernate auto-detects the dialect from the JDBC connection. There is no explicit
  dialect property.
- Actuator exposes only `GET /actuator/health` and `GET /actuator/info`; health details
  are hidden. Point platform probes at `/actuator/health`. Other actuator endpoints
  return `404`. Live and readiness probes share the same endpoint; there are no separate
  liveness/readiness groups.
- CORS is disabled unless `APP_CORS_ALLOWED_ORIGINS` lists one or more exact origins.
  There is no wildcard default.
- `APP_API_KEY` is required. `/recipe/**` calls need the `X-API-Key` header, except
  `GET /recipe/status`; `/actuator/**` and CORS preflight are open. Use a long random
  value from the platform secret store. All clients share it, so rotate it by restarting
  with a new value and updating every client. Terminate TLS in front of the API: the key
  is sent on every request.

## Docker

```bash
docker build -t mediterraneapp .
docker run --rm -p 8080:8080 \
  -e SPRING_DATASOURCE_URL='jdbc:postgresql://host.docker.internal:5432/mediterraneapp' \
  -e SPRING_DATASOURCE_USERNAME='mediterraneapp' \
  -e SPRING_DATASOURCE_PASSWORD='change-me' \
  -e APP_API_KEY='change-me-to-a-long-random-value' \
  mediterraneapp
```

On Linux, `host.docker.internal` is not always available. Use the host's bridge address
or a Docker network that contains both containers.

The Dockerfile is a two-stage build:

1. `maven:3.9-eclipse-temurin-25` runs the committed wrapper
   (`./mvnw -B -ntp -DskipTests package`).
2. `eclipse-temurin:25-jre` copies the exact
   `target/mediterraneapp-0.0.1-SNAPSHOT.jar`, installs `curl`, runs the app as the
   non-root `app` user, and checks `/recipe/status` with a `HEALTHCHECK`.

Known limitations (see [`KNOWN_ISSUES.md`](KNOWN_ISSUES.md)):

- The images are current Temurin 25 tags, but they are not pinned to a digest.
- The build was not exercised locally because Docker is unavailable; validate the image
  in a Docker-capable environment before deploying.

## Heroku (legacy)

`system.properties` sets `java.runtime.version=25`. There is no `Procfile` and no
`app.json`.

Before deploying to Heroku:

1. Create a PostgreSQL add-on or external database.
2. Set config vars:

   ```bash
   heroku config:set SPRING_DATASOURCE_URL='jdbc:postgresql://...' \
     SPRING_DATASOURCE_USERNAME='...' \
     SPRING_DATASOURCE_PASSWORD='...'
   ```

3. Bind to the dyno port. Heroku provides `PORT`, not `SERVER_PORT`. Add
   `server.port: ${PORT:8080}` to `application.yml` (document the change), or use
   `SERVER_PORT` only if your platform assigns a fixed port. A static `SERVER_PORT`
   config var will not follow Heroku's dynamic `PORT`.
4. Confirm the web process. Heroku's Java buildpack normally builds with Maven and
   starts the JAR, but the exact default depends on the buildpack version. A `Procfile`
   such as `web: java $JAVA_OPTS -jar target/mediterraneapp-0.0.1-SNAPSHOT.jar` makes
   it explicit.
5. Do not reuse the historical Heroku database URL or credentials; they remain in Git
   history and are compromised.

Heroku's free PostgreSQL and dyno tiers no longer exist. The old host was removed from
`application.yml` but remains in Git history and should be treated as dead.

## Production checklist

- [ ] If the historical Heroku credentials are still active anywhere, rotate them; they
  remain in Git history.
- [ ] Provide the datasource through the platform's secret store, not
  `application.yml`.
- [x] Use Flyway with `spring.jpa.hibernate.ddl-auto=validate`; add a new versioned
  migration for every schema change.
- [ ] Disable `show-sql` or route it to a controlled logger.
- [x] Require an API key (`APP_API_KEY`) on the recipe API.
- [ ] Add TLS termination in front of the API; the key travels on every request.
- [ ] Add rate limiting at the gateway or proxy (`OPS-5`).
- [ ] Add a health endpoint that checks the database if the platform needs one.
- [ ] Pin and update base images.
- [ ] Add a CI pipeline that builds and tests every change.
- [ ] Back up the database and test a restore.

## Troubleshooting

| Symptom | Likely cause | Fix |
| --- | --- | --- |
| `UnknownHostException` or connection timeout at startup | No datasource is configured, or it points at the retired host | Set `SPRING_DATASOURCE_*` to a reachable database |
| `./mvnw: ... .mvn/wrapper/maven-wrapper.properties: No such file` | Wrapper was incomplete in older commits | Pull the current wrapper files or use a local Maven 3.9+ |
| `Port 8080 was already in use` | Another process owns the port | Set `SERVER_PORT` or stop the other process |
| Tables are missing | Migrations did not run, or the database user cannot create tables | Grant schema privileges and check the Flyway log |
| `IncorrectResultSizeDataAccessException` | Duplicate ingredient triples | Deduplicate ingredients and add a unique constraint |
| Build fails on JDK 24 or older | Boot 4 and the POM require Java 25 | Build with JDK 25 |
| Startup fails with `app.api-key is not set` | `APP_API_KEY` is blank or missing | Set `APP_API_KEY` to a long random value |
| `401 Unauthorized` on `/recipe` | Missing or wrong `X-API-Key` | Send the value of `APP_API_KEY` (not the variable name) |
