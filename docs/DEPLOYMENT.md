# Deployment and local setup

The project is a standard Spring Boot JAR. It can run locally with Maven, in Docker, or
on a Java platform such as Heroku. No database credentials are checked in, so supply the
datasource through the environment; the paths below are templates.

## Prerequisites

- JDK 25 LTS (the POM targets Java 25).
- Maven is not required separately: the committed wrapper downloads Maven 3.9.16.
- PostgreSQL for a real run (12 or newer recommended). The schema is created by Hibernate.
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

2. Export the datasource overrides:

   ```bash
   export SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/mediterraneapp'
   export SPRING_DATASOURCE_USERNAME='mediterraneapp'
   export SPRING_DATASOURCE_PASSWORD='change-me'
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
   curl -i http://localhost:8080/recipe
   # 200, body: []
   ```

The first startup creates the tables because `spring.jpa.hibernate.ddl-auto=update`.
There are no migration scripts.

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
| `JAVA_OPTS` | none | No, used by the Docker `CMD` |

Spring Boot relaxed binding maps `SPRING_DATASOURCE_*` to `spring.datasource.*`.
Environment variables take precedence over `application.yml`.

Other current settings:

- Hikari: `connectionTimeout=20000`, `maximumPoolSize=5`.
- `spring.jpa.show-sql=true`.
- `spring.jpa.hibernate.ddl-auto=update`.
- Hibernate dialect is misspelled and nested under the wrong key; Hibernate
  auto-detects the dialect, so this is harmless today. Fix it in a dedicated change.

## Docker

```bash
docker build -t mediterraneapp .
docker run --rm -p 8080:8080 \
  -e SPRING_DATASOURCE_URL='jdbc:postgresql://host.docker.internal:5432/mediterraneapp' \
  -e SPRING_DATASOURCE_USERNAME='mediterraneapp' \
  -e SPRING_DATASOURCE_PASSWORD='change-me' \
  mediterraneapp
```

On Linux, `host.docker.internal` is not always available. Use the host's bridge address
or a Docker network that contains both containers.

The Dockerfile is a two-stage build:

1. `maven:3.9-eclipse-temurin-25` runs `mvn clean install` inside the image.
2. `eclipse-temurin:25-jre` copies the fat JAR and runs it with `JAVA_OPTS`.

Known weaknesses (see [`KNOWN_ISSUES.md`](KNOWN_ISSUES.md)):

- The images are current Temurin 25 tags, but they are not pinned to a digest.
- `COPY --from=build "MediterraneApp/target/*.jar" app.jar` wraps the wildcard in
  quotes. Docker does not expand a glob inside quotes; verify the copy or use the exact
  JAR name.
- There is no `HEALTHCHECK`, no non-root user, and no `.dockerignore` in the original
  repository (one is now provided).

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
- [ ] Set `spring.jpa.hibernate.ddl-auto=validate` and manage the schema with Flyway or
  Liquibase.
- [ ] Disable `show-sql` or route it to a controlled logger.
- [ ] Add authentication and TLS termination in front of the API.
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
| Tables are missing | Database user cannot create tables, or `ddl-auto` was changed | Grant schema privileges or run migrations |
| `IncorrectResultSizeDataAccessException` | Duplicate ingredient triples | Deduplicate ingredients and add a unique constraint |
| Build fails on JDK 24 or older | Boot 4 and the POM require Java 25 | Build with JDK 25 |
