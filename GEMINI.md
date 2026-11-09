# GEMINI.md

**Read [`AGENTS.md`](AGENTS.md) first.** It is the canonical contract for this
repository. This file is a short pointer for Gemini CLI.

MediterraneApp is a Spring Boot 4.1 / Java 17 REST API for recipes, backed by
PostgreSQL. The project is a legacy portfolio app: it builds and has a test suite plus
CI, but no authentication, and a checked-in database configuration that points at a
retired Heroku instance.

## Key facts

- Run `./mvnw -B verify` before reporting success; it compiles, runs the H2-backed
  test suite, and packages the JAR. CI runs the same command.
- Run with `./mvnw spring-boot:run`, but override the datasource first:
  `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
  `SPRING_DATASOURCE_PASSWORD`.
- The only controller is `RecipeController`, mounted at `/recipe`. There is no
  ingredient endpoint.
- `POST /recipe` upserts by `name`; `PUT /recipe/{uuid}` requires the same UUID in the
  body. Do not change these semantics without documenting a breaking change.
- Never copy the credentials in `src/main/resources/application.yml` into another file,
  a commit, or a response. See [`SECURITY.md`](SECURITY.md).
- Do not upgrade Spring Boot, replace the mappers, or reformat unrelated code as a side
  effect.

## Where to look

- [`AGENTS.md`](AGENTS.md) — canonical rules, architecture, conventions.
- [`docs/API.md`](docs/API.md) — endpoints, payloads, quirks.
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — request flow and persistence.
- [`docs/KNOWN_ISSUES.md`](docs/KNOWN_ISSUES.md) — known defects.
- [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) — local, Docker, Heroku.

For work in another repository, read that repository's own `AGENTS.md` first.
