# Known issues

Prioritized defects and risks in MediterraneApp. Each item includes the evidence and a
suggested fix. Fixes that were already applied in the documentation pass are marked
**fixed**.

Severity:

- **P0** — security or data-loss risk; fix before exposing the service.
- **P1** — blocks reliable use or development.
- **P2** — correctness or maintainability problem.
- **P3** — cleanup.

## Summary

| ID | Severity | Area | Status |
| --- | --- | --- | --- |
| SEC-1 | P0 | Credentials were committed in `application.yml` | **Fixed** |
| OPS-1 | P0 | Configured PostgreSQL instance was retired | **Fixed** |
| API-1 | P1 | Missing recipe returned 500 instead of 404 | **Fixed** |
| API-2 | P1 | Validation was a no-op | **Fixed** |
| API-3 | P1 | `PUT` required the body UUID to match the path | **Fixed** |
| API-4 | P1 | `POST` upserted by name and reported 201 for updates | **Fixed** |
| DATA-1 | P1 | Cascade delete breaks when ingredients are shared | **Fixed** |
| DEV-1 | P1 | No tests | **Fixed** |
| DEV-2 | P1 | No CI | **Fixed** |
| DEV-3 | P3 | Maven wrapper was incomplete | **Fixed** |
| DEV-4 | P1 | Jasypt 2.0.0 could not be resolved | **Fixed** |
| CODE-1 | P2 | `StepRepository` is malformed and unused | **Fixed** |
| DATA-2 | P2 | Ingredient triple has no unique constraint | **Fixed** |
| DATA-3 | P2 | `ddl-auto: update` was the only schema management | **Fixed** |
| DATA-4 | P2 | Recipe updates can orphan previously referenced ingredients | **Fixed** |
| OPS-2 | P2 | Dockerfile uses a fragile wildcard copy | **Fixed** |
| OPS-3 | P2 | No authentication or rate limiting | Open |
| OPS-4 | P2 | No CORS or actuator surface | **Fixed** |
| API-5 | P2 | No pagination on `GET /recipe` | Open |
| API-6 | P2 | `POST /recipe/bulk` was not transactional | **Fixed** |
| API-7 | P2 | Client-supplied UUID on create returned 500 | **Fixed** |
| CFG-1 | P2 | Hibernate dialect is misspelled and misplaced | **Fixed** |
| DEV-5 | P2 | Mapper is constructed manually in the controller | **Fixed** |
| DEV-6 | P3 | `@Autowired` on final fields was redundant | **Fixed** |
| SEC-2 | P3 | Jasypt carried a low-severity advisory | **Fixed** |
| REPO-1 | P3 | `mediterraneapp.iml` was tracked | **Fixed** |
| REPO-2 | P3 | `HELP.md` is stale generated output | **Fixed** |
| REPO-3 | P3 | Trailing-dot Maven `groupId` | Open |

## P0 — security and data loss

### SEC-1 — Credentials were committed in `application.yml`

**Evidence:** `application.yml` used to contain a PostgreSQL host, a database username,
and a Jasypt `ENC(...)` password. The repository is public.

**Fix applied:** removed the URL, username, and password from `application.yml`. The
datasource is supplied through `SPRING_DATASOURCE_*` at runtime, and startup fails
without it. The historical values remain in Git history and in forks, so treat them as
compromised and never reuse them. See [`../SECURITY.md`](../SECURITY.md).

### OPS-1 — Configured PostgreSQL instance was retired

**Evidence:** `application.yml` used to point at an old Heroku PostgreSQL host. Heroku's
free PostgreSQL tier has been discontinued, so the host is unreachable.

**Fix applied:** removed the host from `application.yml`. Provision a local or managed
PostgreSQL database and supply `SPRING_DATASOURCE_*`. See
[`DEPLOYMENT.md`](DEPLOYMENT.md).

## P1 — blocks reliable use

### API-1 — Missing recipe returned 500 instead of 404

**Evidence:** `RecipeService.findRecipeByUuid` and `findIngredientByUuid` called
`orElseThrow()` with no exception mapping. Invalid UUID strings threw
`IllegalArgumentException`.

**Fix applied:** `ApiExceptionHandler` maps `NoSuchElementException` to `404` and
`IllegalArgumentException` to `400`, keeping the default error field names. Integration
tests cover the missing and invalid UUID paths. See [`API.md`](API.md).

### API-2 — Validation was a no-op

**Evidence:** `RecipeController` used `@Valid`, but `RecipeDto` and `IngredientDto`
declared no constraints. Payloads with missing or empty fields reached Hibernate;
syntactically invalid JSON was rejected earlier by Jackson.

**Fix applied:** `RecipeDto.name` and `IngredientDto.nameIngredient` are `@NotBlank`,
`@Size` limits cap recipe name and ingredient fields at 255 characters and each step at
2500, and `RecipeDto.ingredients` cascades validation. The bulk endpoint uses
`List<@Valid RecipeDto>`. `ApiExceptionHandler` maps `MethodArgumentNotValidException`,
`HandlerMethodValidationException`, and `ConstraintViolationException` to `400`.
`spring-boot-starter-validation` stays on the classpath through the web starter.
Oversized input now fails before the database can return a `500`.

### API-3 — `PUT` required the body UUID to match the path

**Evidence:** `RecipeMapper.updateModel` used to set the entity UUID from the body. The
controller loaded the entity by the path UUID, then the mapper overwrote its identifier,
so a missing or different body UUID produced a `500` `identifier ... was altered`.

**Fix applied:** `updateModel` no longer touches the UUID. The controller keeps the
loaded entity's identity, treats the path UUID as authoritative, and returns `400` when
a supplied body UUID does not match. Integration tests cover the matching, omitted, and
mismatched cases.

### API-4 — `POST` upserted by name and returned 201 for updates

**Evidence:** `RecipeController.createRecipe` calls `findRecipeByName`; when a recipe
exists, it updates the same row. The response used to be `201 Created` in both cases.

**Fix applied:** the upsert behavior is kept for compatibility, but the status now
distinguishes the outcomes: `201 Created` on create and `200 OK` on update. Integration
tests assert both. Duplicate names still overwrite data by design; changing the upsert
key needs a migration plan.

### DATA-1 — Cascade delete breaks when ingredients are shared

**Evidence:** `Recipe.ingredients` used `@ManyToMany(cascade = CascadeType.ALL)`.
`CascadeType.ALL` includes `REMOVE`, so deleting a recipe cascaded to every ingredient in
its set.

**Verified behavior before the fix (H2 smoke test):**

- Unshared ingredients were deleted together with the recipe.
- If an ingredient was referenced by another recipe, Hibernate deleted the recipe's join
  rows, then tried to delete the ingredient. The foreign key from `recipe_ingredients`
  rejected the delete, the request returned `500` with a constraint-violation message, and
  the transaction rolled back. The recipe remained.

**Fix applied:** the cascade is now `PERSIST`/`MERGE` only. `RecipeService.deleteRecipe`
runs in a transaction, deletes the recipe, flushes, and then deletes only ingredients
that no remaining recipe references (`existsByIngredients_Uuid`). Shared ingredients are
kept, the delete succeeds with `200 OK` and `"OK"`, and integration tests cover both the
shared and unshared cases. The explicit cleanup is the trade-off; there is no database
cascade.

### DEV-1 — No tests

**Evidence:** there was no `src/test` directory; `./mvnw test` reported "No tests to
run".

**Fix applied:** added 38 tests covering the two mappers, the repositories, the
exception handler, and the full HTTP API. They run on H2 in PostgreSQL mode via the
`test` profile and never touch a remote database. Use `@SpringBootTest(RANDOM_PORT)` plus
`TestRestTemplate` for error responses, because `MockMvc` rethrows unhandled exceptions
instead of rendering the error body. Testcontainers PostgreSQL remains future work.

### DEV-2 — No CI

**Evidence:** there was no `.github/workflows` directory.

**Fix applied:** `.github/workflows/ci.yml` runs `./mvnw -B verify` on Temurin 25 for
pushes and pull requests to `main`, caches the Maven repository, and uploads Surefire
reports when a run fails. Add dependency and container scanning once the build is
stable.

### DEV-4 — Jasypt dependency was unbuildable

**Evidence:** building the original POM failed while reading
`jasypt-spring-boot-starter:2.0.0`, because its POM imported
`spring-cloud-dependencies:Finchley.M8` from `repo.spring.io/libs-milestone`, which now
returns `401 Unauthorized`.

**Fix applied:** the dependency was first moved to `jasypt-spring-boot-starter:4.0.4`,
then removed entirely. The project has no `ENC(...)` values and the datasource is
environment-driven, so the dependency and `@EnableEncryptableProperties` provided no
functionality and carried a low-severity advisory (see SEC-2). If encrypted properties
are introduced later, add a maintained encryption solution and document it.

## P2 — correctness and maintainability

### CODE-1 — `StepRepository` was malformed and unused

**Evidence:** `StepRepository` extended the `org.springframework.stereotype.Repository`
annotation instead of `org.springframework.data.repository.Repository`, and declared type
parameters named `String` and `UUID` that shadowed the real types. It compiled but was
not a Spring Data repository and was never injected.

**Fix applied:** deleted the file. There is no `Step` entity; steps are an
`@ElementCollection` on `Recipe`. If a separate step entity is introduced later, model it
explicitly and extend `JpaRepository<Step, UUID>`.

### DATA-2 — Ingredient triple had no unique constraint

**Evidence:** `findByNameIngredientAndAmountAndMeasurement` assumes uniqueness, but there
was no `@Table(uniqueConstraints = ...)` or database constraint. Duplicate rows caused
`IncorrectResultSizeDataAccessException`.

**Fix applied:** `uk_ingredient_identity` on `(name_ingredient, amount, measurement)`.
Existing duplicates must be deduplicated before the constraint is applied. The entity now
maps `nameIngredient` to the explicit `name_ingredient` column. A regression test asserts
that a duplicate triple violates the constraint. Normalizing ingredient names and units
instead of using free text remains future work.

### DATA-4 — Recipe updates could orphan previously referenced ingredients

**Evidence:** `RecipeService.deleteRecipe` deletes ingredients that no recipe references,
but `upsertRecipe` and `updateRecipe` replaced `Recipe.ingredients` without cleaning up
ingredients that the previous version referenced and nothing else did. Repeated updates
could leave unreachable ingredient rows.

**Fix applied:** `applyUpdate` snapshots the previous ingredient set and, when the request
supplies an `ingredients` field, deletes removed ingredients that no recipe references any
more (same `existsByIngredients_Uuid` check as deletion). An omitted field keeps the old
set and skips cleanup. Integration tests cover the orphaned and shared cases. This
completes the ownership cleanup; the remaining decision is whether ingredient rows should
be normalized or garbage-collected in a migration.

### DATA-3 — `ddl-auto: update` was the only schema management

**Evidence:** `spring.jpa.hibernate.ddl-auto=update`; there were no Flyway or Liquibase
migrations.

**Risks:** implicit changes, no rollback, no reviewable migrations, and destructive
changes are not applied. Not suitable for production.

**Fix applied:** added `spring-boot-starter-flyway` and
`db/migration/V1__baseline.sql`, which matches the current entity model. Hibernate now
runs with `ddl-auto: validate`. `baseline-on-migrate` is enabled with
`baseline-version: 1`, so legacy databases created by `ddl-auto: update` are marked at V1
instead of being recreated. The test profile runs Flyway against H2 and validates the
schema, so every build exercises the migration. Future schema changes need a new
`V2__*.sql`; never edit an applied migration. Migrations have not been run against a live
PostgreSQL server in this environment.

### OPS-2 — Dockerfile used a fragile wildcard copy

**Evidence:**

- `COPY --from=build "MediterraneApp/target/*.jar" app.jar` quoted a wildcard; Docker
  does not expand globs inside quotes.
- The build stage ran `mvn clean install` without skipping tests.
- No `HEALTHCHECK`, no non-root user.

**Fix applied:** the Dockerfile builds with the committed wrapper
(`./mvnw -B -ntp -DskipTests package`), copies the exact JAR
(`target/mediterraneapp-0.0.1-SNAPSHOT.jar`), runs as the non-root `app` user, exposes
port 8080, and adds a `HEALTHCHECK` against `/recipe/status`. The `.dockerignore` keeps
`target/` and IDE state out of the build context. The build was not exercised locally
because Docker is unavailable in this environment; CI validates the Maven build only.

### OPS-3 — No authentication or rate limiting

**Evidence:** no security starter and no rate-limiting filter or gateway configuration.
The API is reachable by anyone who can reach the port.

**Risk:** anyone who can reach the port can read and mutate all data, and can exhaust
resources with repeated requests.

**Fix:** add authentication before exposing the API publicly, then add rate limiting at
the gateway or with a filter. The mechanism (API key, OAuth2 resource server, session) is
a product decision; document the chosen scheme here and in the API reference.

### OPS-4 — No CORS or actuator surface

**Evidence:** there was no CORS configuration and no actuator dependency, so a browser
client could not call the API cross-origin and there was no health endpoint for a
platform probe.

**Fix applied:** `spring-boot-starter-actuator` exposes only `health` and `info`, with
health details hidden. `CorsConfig` enables `/recipe/**` for the origins listed in
`APP_CORS_ALLOWED_ORIGINS`; an empty list disables CORS, so same-origin clients are
unaffected. Integration tests assert the health body, the `404` for an unexposed
endpoint, a successful preflight for a configured origin, and `403` for an unknown one.

### API-5 — No pagination on `GET /recipe`

**Evidence:** `recipeService.getAllRecipe()` calls `findAll()` and returns every row.

**Fix:** accept `page` and `size` (or cursor parameters), return a page envelope, and
document the change. This is a response-shape change; coordinate it with clients.

### API-6 — `POST /recipe/bulk` was not transactional

**Evidence:** the controller looped over the create logic with no transaction. A failure
halfway left earlier items saved.

**Fix applied:** `RecipeService.upsertRecipes` is `@Transactional`, so the whole batch
succeeds or fails together. Validation still runs before the service call, so an invalid
item returns `400` and writes nothing. Integration tests cover both the invalid-item path
and the all-or-nothing contract. Returning per-item results remains a possible future
change.

### API-7 — Client-supplied UUID on create returned 500

**Evidence:** `doUpsert` copied a client-supplied `uuid` onto a new entity with a
generated identifier. Spring Data saw a non-null ID and used `merge`, which tried to
update a row that did not exist; the request returned `500`
(`ObjectOptimisticLockingFailureException`). The documentation claimed the value was
accepted, but no test covered it.

**Fix applied:** the identifier is always server-generated and a client-supplied `uuid`
is ignored on create. `POST` still upserts by name and returns the generated UUID.
An integration test asserts the supplied value is not used and that the generated row
exists.

### CFG-1 — Hibernate dialect was misspelled and misplaced

**Evidence:**

```yaml
spring:
  jpa:
    hibernate:
      dialect: org.hibernate.dialect.PostgreSQLDialec
```

The value was truncated, and `dialect` belongs under
`spring.jpa.properties.hibernate.dialect`. Hibernate auto-detected the dialect, so the
application still worked.

**Fix applied:** removed the line. Hibernate auto-detects the dialect from the JDBC
connection, which is preferable with a single known database. If an explicit dialect is
needed later, set it under `spring.jpa.properties.hibernate.dialect`.

### DEV-5 — Mapper was constructed manually in the controller

**Evidence:** `RecipeController` did `this.recipeMapper = new RecipeMapper(...)`, while
`RecipeMapper` also had an `@Autowired` field that was ignored because the mapper was not
a Spring bean.

**Fix applied:** `RecipeMapper` is a `@Component`; the controller and service receive it
through constructor injection, and the ignored `@Autowired` field was removed.
`IngredientMapper` is still instantiated directly.

## P3 — cleanup

### SEC-2 — Jasypt carried a low-severity advisory

**Evidence:** Dependabot reported GHSA-jgj7-c8vj-w563 / CVE-2026-9370 (low) for
`jasypt-spring-boot` and `jasypt-spring-boot-starter` through 4.0.4; no patched version
was available.

**Fix applied:** removed Jasypt, which was unused. No configuration encryption is
configured; supply secrets through the platform's secret store. Re-check the advisory if
an encryption library is added later.

### DEV-3 — Maven wrapper was incomplete

**Evidence:** the original repository committed `mvnw` and `mvnw.cmd` but not
`.mvn/wrapper/maven-wrapper.properties`, so `./mvnw` failed before Maven started.

**Fix applied:** regenerated the script-only wrapper with Maven Wrapper 3.3.4 and Maven
3.9.16. `./mvnw -v` and `./mvnw -B -DskipTests package` now work.

### DEV-6 — `@Autowired` on final fields was redundant

**Evidence:** `RecipeController` annotated final fields that are assigned in the
constructor. Constructor injection already satisfies the dependency.

**Fix applied:** the redundant annotations were removed. The ignored `@Autowired` field
in `RecipeMapper` remains part of DEV-5, because the mapper is still constructed
manually.

### REPO-1 — `mediterraneapp.iml` was tracked

**Evidence:** the IntelliJ module file was committed even though `.gitignore` lists
`*.iml` (tracked files are not affected by ignore rules).

**Fix applied:** removed the file. `.gitignore` still ignores `*.iml`; regenerate it
locally if needed.

### REPO-2 — `HELP.md` was stale generated output

**Evidence:** Spring Initializr generated `HELP.md`, which pointed at old Spring Boot
documentation and added nothing beyond [`README.md`](../README.md).

**Fix applied:** deleted it. Treat this file as the project documentation index instead.

### REPO-3 — Trailing-dot Maven `groupId`

**Evidence:** the POM uses `<groupId>com.ges.</groupId>` while the package is
`com.ges.mediterraneapp`. Maven accepts it, but the trailing dot is unusual and can
confuse tooling.

**Fix:** leave it unless you control every consumer. Changing it changes the published
coordinates and any dependency references.

## Verification notes

- Build verified with Temurin 25.0.5+8 and Maven Wrapper 3.9.16 (Spring Boot 4.1.2):
  `./mvnw -B -DskipTests package` → `BUILD SUCCESS`.
- `./mvnw test` → 38 tests, 0 failures, `BUILD SUCCESS`.
- Spring Boot 4 / Jackson 3 serialize enums with `toString()`, which would have changed
  the `"I_AM_A_TEAPOT"`, `"CREATED"`, and `"OK"` bodies. `JacksonConfig` registers an
  `HttpStatus` serializer that writes the enum name.
- No database credentials are committed; the datasource is supplied through
  `SPRING_DATASOURCE_*` at runtime.
- Actuator exposes only `health` and `info`, with health details hidden. CORS is opt-in
  through `APP_CORS_ALLOWED_ORIGINS` and adds no mapping when the list is empty.
- Jasypt and `@EnableEncryptableProperties` were removed; the build no longer carries the
  low-severity Jasypt advisory.
- The integration tests exercise the API against H2 in PostgreSQL mode (create, read,
  upsert, bulk including rollback, validation, missing row returning 404, invalid UUID
  returning 400, PUT with matching/omitted/mismatched UUID, delete with 404 and orphan
  cleanup, update cleanup for dropped and shared ingredients, shared-ingredient delete,
  ignored client-supplied UUID on create, and unique-constraint conflicts returning 409).
  The same behavior is expected on PostgreSQL, but it was not exercised against a live
  PostgreSQL server.
- Flyway applies `db/migration/V1__baseline.sql` before Hibernate starts, and
  `ddl-auto: validate` rejects entity/schema drift. The test profile runs the same
  migration against H2.
- UUIDs use Hibernate's `@UuidGenerator`; the deprecated `@GenericGenerator(uuid2)` and
  `HttpStatus.I_AM_A_TEAPOT` constant were removed. The status endpoint uses
  `HttpStatus.valueOf(418)` and still writes `"I_AM_A_TEAPOT"`.
- The Dockerfile was hardened but not built locally: Docker is not available in this
  environment. Validate it in a Docker-capable environment before deploying.
