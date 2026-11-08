# GitHub Copilot instructions

The canonical guidance for this repository is [`../AGENTS.md`](../AGENTS.md). Read it
first; this file is only a short pointer.

MediterraneApp is a **Spring Boot 2.2 / Java 11 REST API for recipes**, backed by
PostgreSQL. It is a legacy portfolio project: it builds, but has no tests, no CI, no
authentication, and a checked-in database configuration that points at a retired Heroku
instance.

Key points:

- Build with `./mvnw -B -DskipTests package`; run tests with `./mvnw test` (currently
  zero tests).
- The only controller is `RecipeController` at `/recipe`. There is no ingredient
  endpoint.
- `POST /recipe` upserts by `name`. `PUT /recipe/{uuid}` requires the same UUID in the
  body; a missing or different UUID returns 500 and leaves the row unchanged. Preserve
  these semantics unless a breaking change is explicitly requested.
- Never copy the credentials from `src/main/resources/application.yml` into code,
  docs, tests, commits, or chat. See [`../SECURITY.md`](../SECURITY.md).
- Keep the existing Java 11 / Spring Boot 2.2 stack, hand-written mappers, and API
  shape unless the owner asks for a migration.
- Run `./mvnw -B -DskipTests package` and `git diff --check` before reporting success.
  Do not claim a check passed unless it was run.

Details: [`../AGENTS.md`](../AGENTS.md), [`../docs/API.md`](../docs/API.md),
[`../docs/ARCHITECTURE.md`](../docs/ARCHITECTURE.md),
[`../docs/KNOWN_ISSUES.md`](../docs/KNOWN_ISSUES.md).

For work in another repository, read that repository's own `AGENTS.md` first.
