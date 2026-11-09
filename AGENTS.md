# AGENTS.md

Canonical guidance for AI coding agents (OpenCode, GitHub Copilot, Cursor, Claude Code,
Gemini CLI, Codex, and similar tools) working in this repository. Read this file before
changing code.

## Project snapshot

| Field | Value |
| --- | --- |
| What it is | REST API for recipes, their ingredients, and preparation steps |
| Language | Java 25 (LTS) |
| Framework | Spring Boot 4.1.2 (Spring MVC, Spring Data JPA, Hibernate 7.4) |
| Build | Maven via the committed wrapper (`./mvnw`, Maven 3.9.16) |
| Database | PostgreSQL; schema managed by Hibernate `ddl-auto: update` |
| Libraries | Lombok 1.18.48 (Boot BOM), Jasypt Spring Boot 4.0.4, Jackson 3 |
| Size | 13 main Java files, about 460 lines, 20 tests |
| Status | Legacy portfolio project (2020), not production-ready |
| License | GPL-3.0 (see `LICENSE`) |

Root package: `com.ges.mediterraneapp`. The Maven `groupId` is `com.ges.` with a
trailing dot. Do not "fix" one to match the other without checking how the artifact is
consumed.

## Current state

- `./mvnw clean package` succeeds on JDK 25 with Spring Boot 4.1.2 and Jasypt
  4.0.4. The upgrade from Boot 2.2.6 moved `javax.*` imports to `jakarta.*` and added
  `spring-boot-starter-validation`, which Boot 4 no longer pulls in transitively.
- JDK 23+ no longer runs annotation processors implicitly. The POM lists Lombok under
  `maven-compiler-plugin` → `annotationProcessorPaths`; keep that configuration when the
  compiler plugin changes, or Lombok-generated accessors disappear.
- Jackson 3 serializes enums with `toString()`, which would change the historical
  bodies (`"I_AM_A_TEAPOT"`, `"CREATED"`, `"OK"`). `JacksonConfig` registers an
  `HttpStatus` serializer that writes `name()`, so the controller keeps returning the
  enum types. Do not replace that with `HttpStatus.*.name()` strings: a `String` return
  value is served as `text/plain` without quotes.
- The test suite covers the mappers, the repository, and the HTTP API. It runs against
  H2 in PostgreSQL mode through the `test` profile; `./mvnw test` needs no database.
- `src/main/resources/application.yml` contains no database credentials. Supply the
  datasource through `SPRING_DATASOURCE_*` (or a local, untracked profile); the
  application will not start without it.
- GitHub Actions runs `./mvnw -B verify` on every push and pull request to `main`.
  There is no migration tool, authentication, actuator, OpenAPI document, or API
  versioning.

See [`docs/KNOWN_ISSUES.md`](docs/KNOWN_ISSUES.md) for the prioritized list.

## Ground rules

1. **Never print, copy, log, or commit real secrets.** `application.yml` intentionally
   contains no datasource credentials; use `SPRING_DATASOURCE_*` or a local, untracked
   profile. Use placeholders in docs, tests, issues, and commits. Historical Heroku
   values remain in Git history and must be treated as compromised.
2. **Never run tests or the application against a remote database.** The tests use an
   in-memory H2 database through the `test` profile. For a real run, use a local
   PostgreSQL instance and supply the datasource through environment variables or a
   local profile.
3. **Preserve the public API shape.** Endpoint paths, JSON property names, and HTTP
   methods are consumed by clients. Additive changes are preferred; breaking changes
   must be called out explicitly.
4. **Make the smallest change that solves the problem.** Do not reformat unrelated
   files, rename packages, or upgrade the framework as a side effect.
5. **Stay on Java 25 (LTS) and Spring Boot 4.1.x** unless the owner explicitly asks for a
   migration. The code uses the `jakarta.*` namespace; do not reintroduce `javax.*`
   imports.
6. **Do not reintroduce MapStruct.** The Git history contains abandoned MapStruct
   commits; the current code uses hand-written mappers. Keep it that way unless asked.
7. **Do not add `@Transactional` blindly** but do add it to service methods that write
   more than one entity, and never make a controller method transactional.
8. **Keep the DTO boundary.** Entities must not leak out of controllers. Convert with
   the existing mappers or a new mapper in the same package.
9. **Do not push to `main`, force-push, rewrite history, or publish releases** unless
   the owner explicitly asks.
10. **Update the owning document when behavior changes.** If you change an endpoint,
    update [`docs/API.md`](docs/API.md). If you change a design decision, update
    [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md). If you fix a listed bug, update
    [`docs/KNOWN_ISSUES.md`](docs/KNOWN_ISSUES.md).

## Read order

Before changing anything, read:

1. `AGENTS.md` (this file).
2. [`README.md`](README.md) — what the project is and how to run it.
3. [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — layers, request flow, persistence.
4. [`docs/API.md`](docs/API.md) — endpoints, payloads, and current quirks.
5. [`docs/KNOWN_ISSUES.md`](docs/KNOWN_ISSUES.md) — what is already broken.
6. [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) — Docker, Heroku history, configuration.

## Quick start

Prerequisites: JDK 25 (LTS) and network access for the Maven wrapper and dependencies.
PostgreSQL is required to actually start the application (12+ recommended).

```bash
# Compile and package (verified on Temurin 25.0.5+8)
./mvnw -B clean package

# Run the test suite (H2, no PostgreSQL required)
./mvnw test

# Start the API on http://localhost:8080
./mvnw spring-boot:run
```

The configuration has no datasource credentials, so set them before the last command:

```bash
export SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/mediterraneapp'
export SPRING_DATASOURCE_USERNAME='mediterraneapp'
export SPRING_DATASOURCE_PASSWORD='change-me'
```

A disposable local database:

```bash
docker run --rm --name mediterraneapp-db \
  -e POSTGRES_DB=mediterraneapp \
  -e POSTGRES_USER=mediterraneapp \
  -e POSTGRES_PASSWORD=change-me \
  -p 5432:5432 -d postgres:16
```

Smoke test:

```bash
curl -i http://localhost:8080/recipe/status
# HTTP/1.1 200
# "I_AM_A_TEAPOT"
```

Docker:

```bash
docker build -t mediterraneapp .
docker run --rm -p 8080:8080 \
  -e SPRING_DATASOURCE_URL='jdbc:postgresql://host.docker.internal:5432/mediterraneapp' \
  -e SPRING_DATASOURCE_USERNAME='mediterraneapp' \
  -e SPRING_DATASOURCE_PASSWORD='change-me' \
  mediterraneapp
```

The Dockerfile builds with `maven:3.9-eclipse-temurin-25` and runs on
`eclipse-temurin:25-jre`.

## Configuration

`src/main/resources/application.yml` is the only configuration file. Spring Boot relaxed
binding means environment variables override it:

| Environment variable | Property | Notes |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `spring.datasource.url` | JDBC URL, required at startup |
| `SPRING_DATASOURCE_USERNAME` | `spring.datasource.username` | Required at startup; no value is committed |
| `SPRING_DATASOURCE_PASSWORD` | `spring.datasource.password` | Required at startup; no value is committed |
| `JASYPT_ENCRYPTOR_PASSWORD` | `jasypt.encryptor.password` | Only needed if an `ENC(...)` value is added |
| `SERVER_PORT` | `server.port` | Defaults to `8080` |

- `.env.example` lists the variable names. Spring Boot does **not** read `.env`
  automatically; export the variables, use `direnv`, or pass `--env-file` to Docker.
- `spring.jpa.hibernate.ddl-auto` is `update`. Hibernate creates and alters tables from
  the entities. There is no Flyway or Liquibase.
- `spring.jpa.show-sql` is `true`, so SQL is printed to the console.
- The configured Hibernate dialect has a typo and is in the wrong property namespace.
  Hibernate auto-detects the dialect, so the application still works. Do not copy the
  current line as a pattern.

## Architecture at a glance

```text
HTTP
  │
  ▼
RecipeController                 @RestController, /recipe
  │  DTO in / DTO out
  ▼
RecipeService / IngredientService
  │  entities
  ▼
RecipeRepository / IngredientRepository   Spring Data JPA
  │
  ▼
PostgreSQL

RecipeMapper / IngredientMapper  hand-written entity <-> DTO conversion
```

| Path | Responsibility |
| --- | --- |
| `src/main/java/com/ges/mediterraneapp/MediterraneappApplication.java` | Spring Boot entry point; enables Jasypt-encrypted properties. |
| `controller/RecipeController.java` | The only controller. All `/recipe` endpoints. |
| `service/RecipeService.java` | Recipe lookup, create, update, delete. |
| `service/IngredientService.java` | Ingredient lookup, create, update, delete. No controller exposes it yet. |
| `mapper/RecipeMapper.java` | Entity/DTO mapping, including resolving ingredient DTOs to entities. |
| `mapper/IngredientMapper.java` | Entity/DTO mapping for ingredients. |
| `model/dao/Recipe.java` | JPA entity: recipe, steps, ingredient relation, timestamps. |
| `model/dao/Ingredient.java` | JPA entity: amount, measurement, ingredient name, timestamps. |
| `model/dao/repository/RecipeRepository.java` | `findByUuid`, `findByName`. |
| `model/dao/repository/IngredientRepository.java` | `findByUuid`, `findByNameIngredientAndAmountAndMeasurement`. |
| `model/dao/repository/StepRepository.java` | Unused, malformed placeholder; see known issues. |
| `model/dto/RecipeDto.java` | JSON shape for recipes; `NON_NULL` inclusion. |
| `model/dto/IngredientDto.java` | JSON shape for ingredients; `NON_NULL` inclusion. |
| `src/main/resources/application.yml` | Datasource, Hikari pool, JPA/Hibernate settings. |

There is no `IngredientController`, no service interfaces, and no `@ControllerAdvice`.

### Request flow

1. Jackson 3 deserializes the body into a `RecipeDto`.
2. `RecipeController` calls `RecipeService` to load or create the entity.
3. `RecipeMapper.updateModel` copies fields and resolves each `IngredientDto` through
   `IngredientService.findIngredientByNameAndAmountAndMeasurement`. A missing ingredient
   becomes a new transient `Ingredient`.
4. `RecipeService` saves the entity. `CascadeType.ALL` on the ingredient relation
   persists new ingredients and join rows.
5. `RecipeMapper.toDto` converts the saved entity back to JSON.

Reads are simpler: controller → service → repository → mapper → DTO.

## Data model

`Recipe`

| Field | Type | Notes |
| --- | --- | --- |
| `uuid` | `UUID` | Primary key, generated (`uuid2`). Exposed as a string. |
| `name` | `String` | Used as the de facto upsert key by `POST /recipe`. |
| `relations` | `String` | Free-text field; meaning is undocumented. |
| `preparedTime` | `String` | Free text, not a duration type. |
| `cookedTime` | `String` | Free text. |
| `totalTime` | `String` | Free text. |
| `steps` | `Set<String>` | `@ElementCollection`, column length 2500. |
| `ingredients` | `Set<Ingredient>` | Unidirectional `@ManyToMany(cascade = ALL)`. |
| `createdAt` | `Date` | Hibernate `@CreationTimestamp`. |
| `updatedAt` | `Date` | Hibernate `@UpdateTimestamp`. |

`Ingredient`

| Field | Type | Notes |
| --- | --- | --- |
| `uuid` | `UUID` | Primary key, generated. |
| `amount` | `String` | Free text (`"2"`, `"a pinch"`). |
| `measurement` | `String` | Free text (`"kg"`, `"tbsp"`). |
| `nameIngredient` | `String` | Ingredient name; not unique. |
| `createdAt` / `updatedAt` | `Date` | Hibernate timestamps. |

Important persistence facts:

- The ingredient lookup key is the triple `(nameIngredient, amount, measurement)`, but
  the database has no unique constraint on it. Duplicate rows make the repository method
  throw `IncorrectResultSizeDataAccessException`.
- `CascadeType.ALL` includes `REMOVE`: deleting a recipe deletes its unshared
  ingredients. If an ingredient is shared with another recipe, the foreign key blocks
  the delete, the request returns 500, and the transaction rolls back. Do not change
  this behavior without understanding the data.
- `steps` and the many-to-many join table are created by Hibernate with generated names.
- There is no pagination; `GET /recipe` loads the whole table.

## API surface

All paths are under `/recipe`. Full payloads and status codes are in
[`docs/API.md`](docs/API.md).

| Method | Path | Behavior |
| --- | --- | --- |
| `GET` | `/recipe/status` | Returns `"I_AM_A_TEAPOT"` as a JSON string with HTTP 200. |
| `GET` | `/recipe` | Returns every recipe. |
| `GET` | `/recipe/{uuid}` | Returns one recipe, or 500 when absent. |
| `POST` | `/recipe` | Creates a recipe, or updates an existing one with the same `name`. |
| `POST` | `/recipe/bulk` | Creates/updates each item, returns `"CREATED"` with HTTP 200. |
| `PUT` | `/recipe/{uuid}` | Updates a recipe; the body should repeat the path UUID. |
| `DELETE` | `/recipe/{uuid}` | Deletes a recipe, returns `"OK"` with HTTP 200. |

Current quirks that must be preserved unless the owner asks for a breaking change:

- `POST /recipe` upserts by `name`, not by UUID.
- `PUT /recipe/{uuid}` maps `RecipeDto.uuid` onto the entity. A missing or mismatched
  body UUID makes Hibernate throw `identifier ... was altered` and the request returns
  500. Always send the path UUID in the body.
- `@Valid` is present but the DTOs have no constraint annotations, so it is a no-op.
- Missing rows surface as `NoSuchElementException` and Spring's default 500 response.
- Invalid UUID strings throw `IllegalArgumentException` and also surface as 500.
- `DELETE /recipe/{uuid}` cascades `REMOVE` to ingredients. An ingredient shared with
  another recipe causes a foreign-key violation and a 500 response that rolls back the
  delete.
- Returning `HttpStatus` from a `@RestController` method writes the enum name as the
  JSON response body instead of setting the response status. `JacksonConfig` pins that
  name (`name()` rather than Jackson 3's `toString()`).

## Conventions

- **Packages:** `controller`, `service`, `mapper`, `model.dao`, `model.dao.repository`,
  `model.dto` under `com.ges.mediterraneapp`. Keep the existing layer boundaries.
- **Formatting:** 4 spaces for Java, LF line endings, final newline. `.editorconfig`
  covers the defaults; the POM historically uses tabs.
- **Lombok:** entities and DTOs use `@Data`; services use `@Slf4j`. Do not add boilerplate
  getters/setters that Lombok already generates.
- **DTOs:** UUIDs are strings at the boundary. `@JsonInclude(NON_NULL)` means null
  fields are omitted. Do not expose entities directly.
- **Mappers:** plain classes, constructed in the controller for `RecipeMapper`.
  `IngredientMapper.toDto` is package-private on purpose. If you convert mappers to
  Spring beans, do it in a dedicated change and update this file.
- **Repositories:** Spring Data derived queries; no JPQL or native SQL yet.
- **Controllers:** constructor injection is preferred for new code. The existing
  `@Autowired` fields in `RecipeController` are redundant but harmless.
- **Errors:** if you add exception handling, add one `@RestControllerAdvice` with
  explicit status mappings; do not add try/catch blocks to each endpoint.
- **JSON naming:** camelCase, matching the DTO fields. Do not add snake_case aliases.

## Testing

Tests live under `src/test/java/com/ges/mediterraneapp/` and run on H2 in PostgreSQL
mode through `src/test/resources/application-test.yml` (profile `test`). They never
touch a remote database. JUnit 5, Mockito, AssertJ, JsonPath, and the Boot
test starters are on the test classpath.

Current coverage (20 tests):

- `mapper/RecipeMapperTest` and `mapper/IngredientMapperTest` — field mapping,
  null-collection behavior, and ingredient resolution.
- `model/dao/repository/RecipeRepositoryTest` — `@DataJpaTest` lookups by UUID and name.
- `RecipeApiIntegrationTest` — full HTTP behavior with `@SpringBootTest(RANDOM_PORT)`
  and `TestRestTemplate`, so 500 responses go through the real error dispatch. It covers
  status, create, list, get, missing/invalid UUID, bulk, upsert-by-name, PUT, PUT UUID
  mismatch, delete, and shared-ingredient rollback.

Guidance:

- Prefer testing behavior over implementation. Keep the API assertions in
  `RecipeApiIntegrationTest` aligned with [`docs/API.md`](docs/API.md).
- Use `@DataJpaTest` for repositories and unit tests for the hand-written mappers.
- Use the real servlet container (`RANDOM_PORT`), not `MockMvc`, when asserting error
  responses: `MockMvc` rethrows unhandled exceptions instead of rendering the 500 body.
- Keep `spring.jpa.open-in-view` at its production default. Disabling it for tests
  triggers `LazyInitializationException` and detached-entity errors that production does
  not have.
- Every bug fix should add a regression test when practical.
- Run `./mvnw -B verify` before reporting success. Do not claim a check passed unless it
  was actually run.

## Security

- No endpoint has authentication or authorization. Do not expose this API publicly
  without adding both.
- No CORS configuration exists. Add it explicitly if a browser client is introduced.
- Input is not validated despite `@Valid`. Add Bean Validation constraints to the DTOs
  (and a handler for `MethodArgumentNotValidException`) when hardening the API.
- Never log request bodies blindly; they can contain data that should not be retained.
- Do not commit `.env`, keystores, Jasypt keys, or real database credentials. `.gitignore`
  excludes `.env`; `.env.example` contains placeholders only.
- If a credential is exposed, rotate it. Do not rely on rewriting history; see
  [`SECURITY.md`](SECURITY.md).

## Known issues

The prioritized list lives in [`docs/KNOWN_ISSUES.md`](docs/KNOWN_ISSUES.md). Highlights:

1. Historical Heroku credentials remain in Git history; no credentials are checked in.
2. Missing rows return 500 instead of 404.
3. Validation is a no-op.
4. `POST`/`PUT` UUID and upsert semantics are surprising.
5. `StepRepository` is unused and malformed.
6. Dockerfile relies on a wildcard copy for the packaged JAR.

## Change workflow

1. Read this file and the relevant document in `docs/`.
2. State the scope and whether the change is a breaking API change.
3. Make the smallest change.
4. Run `./mvnw -B verify` (compile, tests, and packaging).
5. Run `git diff --check`.
6. Update the owning docs if behavior or design changed.
7. Report what changed, what was validated, and what remains unverified.

Use a branch and pull request for non-trivial work. Do not commit generated artifacts
(`target/`), IDE state, or secrets.

## Definition of Done

- [ ] The requested change is complete and scoped.
- [ ] `./mvnw -B verify` succeeds.
- [ ] The test suite was run, or the reason it could not run is stated.
- [ ] No secrets, credentials, or local configuration were added.
- [ ] The API shape is unchanged, or the breaking change is documented.
- [ ] `docs/API.md` and `docs/ARCHITECTURE.md` still match the code.
- [ ] `git diff --check` is clean.
- [ ] Unrelated files were not reformatted.

## Out of scope unless explicitly requested

- Upgrading to Spring Boot 3 / Jakarta EE.
- Replacing Hibernate `ddl-auto` with Flyway or Liquibase.
- Adding authentication, a frontend, or API versioning.
- Rewriting the mappers with MapStruct.
- Changing the license.
- Force-pushing, history rewriting, or deleting branches.
- Deploying to a real environment.

## AI context files

| File | Purpose |
| --- | --- |
| `AGENTS.md` | Canonical contract (this file). |
| `CLAUDE.md` | Short pointer for Claude Code. |
| `GEMINI.md` | Short pointer for Gemini CLI. |
| `.github/copilot-instructions.md` | Short pointer for GitHub Copilot. |
| `.cursor/rules/project.mdc` | Short pointer for Cursor. |
| `llms.txt` | LLM-friendly index of the repository. |
| `docs/API.md` | Endpoint reference. |
| `docs/ARCHITECTURE.md` | Design and persistence reference. |
| `docs/DEPLOYMENT.md` | Local, Docker, and legacy Heroku notes. |
| `docs/KNOWN_ISSUES.md` | Prioritized defects and fixes. |

## Related files

- [`README.md`](README.md) — human-facing overview.
- [`CONTRIBUTING.md`](CONTRIBUTING.md) — contribution workflow.
- [`SECURITY.md`](SECURITY.md) — private vulnerability reporting.
- [`docs/API.md`](docs/API.md) — API reference.
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — architecture.
- [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) — deployment.
- [`docs/KNOWN_ISSUES.md`](docs/KNOWN_ISSUES.md) — known issues.
- [`.env.example`](.env.example) — configuration variable names.
