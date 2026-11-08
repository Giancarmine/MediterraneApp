# Contributing

Thanks for taking a look at MediterraneApp. This is a small legacy project, so changes
should stay focused and easy to review.

## Before you start

- Read [`AGENTS.md`](AGENTS.md) for the architecture and non-negotiable rules.
- Read [`docs/KNOWN_ISSUES.md`](docs/KNOWN_ISSUES.md); the bug you found may already be
  documented with a suggested fix.
- For a non-trivial change, open an issue first so the approach can be agreed on.

## Setup

Prerequisites: JDK 11 and PostgreSQL 12+. The Maven wrapper is committed; do not replace
it with a system Maven in build instructions.

```bash
./mvnw -B clean package
./mvnw test
```

To run the API, override the datasource (the checked-in values are historical and
unreachable):

```bash
export SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/mediterraneapp'
export SPRING_DATASOURCE_USERNAME='mediterraneapp'
export SPRING_DATASOURCE_PASSWORD='change-me'
./mvnw spring-boot:run
```

See [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) for a disposable PostgreSQL container and
Docker instructions.

## Making a change

1. Create a branch from `main`.
2. Keep the diff focused. Do not reformat unrelated files or upgrade dependencies as a
   side effect.
3. Preserve the existing API behavior unless the change is explicitly a breaking one.
   `POST /recipe` upserts by `name`; `PUT /recipe/{uuid}` expects the same UUID in the
   body.
4. Add or update tests when practical. The test tree does not exist yet; see the testing
   section of [`AGENTS.md`](AGENTS.md).
5. Update the owning documentation:
   - endpoint or payload change → [`docs/API.md`](docs/API.md);
   - design or persistence change → [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md);
   - fixed or newly discovered defect → [`docs/KNOWN_ISSUES.md`](docs/KNOWN_ISSUES.md).
6. Run the checks:

   ```bash
   ./mvnw -B -DskipTests package
   ./mvnw test
   git diff --check
   ```

7. Open a pull request and describe what changed, why, and what you validated.

## Commit messages

The history uses short imperative summaries such as `Add recipe CRUD`, `Fix Pom`, and
`Mod update recipe`. Follow that style:

```text
Add ingredient endpoint
Fix missing recipe response
Update API documentation
```

Do not use a commit message as the only place where a decision is explained; put the
rationale in the pull request or the relevant document.

## Code style

- Java 11, 4-space indentation, LF line endings, final newline.
- Follow the existing package layout: `controller`, `service`, `mapper`, `model.dao`,
  `model.dao.repository`, `model.dto`.
- Use Lombok for entities and DTOs; do not add boilerplate accessors.
- Keep DTOs at the HTTP boundary; do not return entities from controllers.
- Use the existing hand-written mappers. Do not add MapStruct or ModelMapper.
- Do not add secrets or real local configuration. `.env` is ignored;
  `.env.example` contains placeholders only.

## Security

Never include real credentials in a pull request, issue, test, or log. If you discover a
leaked credential, follow [`SECURITY.md`](SECURITY.md) instead of opening a public issue.
See [`SECURITY.md`](SECURITY.md) for private reporting.

## What not to do

- Do not rewrite history, force-push, or delete branches.
- Do not upgrade to Spring Boot 3, switch to Jakarta, or migrate the schema as part of
  an unrelated fix.
- Do not run tests against the configured remote database.
- Do not change the license.
- Do not commit `target/`, IDE state, or generated files.
