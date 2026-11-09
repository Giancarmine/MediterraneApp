# GitHub Copilot instructions

The canonical guidance for this repository is [`../AGENTS.md`](../AGENTS.md). Read it
first; this file is only a short pointer.

MediterraneApp is a **Spring Boot 4.1 / Java 17 REST API for recipes**, backed by
PostgreSQL. It is a legacy portfolio project: it builds and has a test suite plus CI,
but no authentication. No database credentials are checked in; supply
`SPRING_DATASOURCE_*` at runtime.

Key points:

- Run `./mvnw -B verify` before reporting success; it compiles, runs the H2-backed
  test suite, and packages the JAR. CI runs the same command.
- The only controller is `RecipeController` at `/recipe`. There is no ingredient
  endpoint.
- `POST /recipe` upserts by `name`. `PUT /recipe/{uuid}` requires the same UUID in the
  body; a missing or different UUID returns 500 and leaves the row unchanged. Preserve
  these semantics unless a breaking change is explicitly requested.
- Never add credentials to `src/main/resources/application.yml` or copy them into code,
  docs, tests, commits, or chat. See [`../SECURITY.md`](../SECURITY.md).
- Keep the existing Java 17 / Spring Boot 4.1 stack, hand-written mappers, and API
  shape unless the owner asks for a migration.
- Run `./mvnw -B verify` and `git diff --check` before reporting success. Do not claim
  a check passed unless it was run.

Details: [`../AGENTS.md`](../AGENTS.md), [`../docs/API.md`](../docs/API.md),
[`../docs/ARCHITECTURE.md`](../docs/ARCHITECTURE.md),
[`../docs/KNOWN_ISSUES.md`](../docs/KNOWN_ISSUES.md).

For work in another repository, read that repository's own `AGENTS.md` first.
