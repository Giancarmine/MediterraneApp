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
| DATA-1 | P1 | Cascade delete breaks when ingredients are shared | Open |
| DEV-1 | P1 | No tests | **Fixed** |
| DEV-2 | P1 | No CI | **Fixed** |
| DEV-3 | P3 | Maven wrapper was incomplete | **Fixed** |
| DEV-4 | P1 | Jasypt 2.0.0 could not be resolved | **Fixed** |
| CODE-1 | P2 | `StepRepository` is malformed and unused | Open |
| DATA-2 | P2 | Ingredient triple has no unique constraint | Open |
| DATA-3 | P2 | `ddl-auto: update` is the only schema management | Open |
| OPS-2 | P2 | Dockerfile uses a fragile wildcard copy | Open |
| OPS-3 | P2 | No auth, CORS, actuator, or rate limiting | Open |
| API-5 | P2 | No pagination on `GET /recipe` | Open |
| API-6 | P2 | `POST /recipe/bulk` is not transactional | Open |
| CFG-1 | P2 | Hibernate dialect is misspelled and misplaced | Open |
| DEV-5 | P2 | Mapper is constructed manually in the controller | Open |
| DEV-6 | P3 | `@Autowired` on final fields is redundant | Open |
| SEC-2 | P3 | Jasypt carried a low-severity advisory | **Fixed** |
| REPO-1 | P3 | `mediterraneapp.iml` was tracked | **Fixed** |
| REPO-2 | P3 | `HELP.md` is stale generated output | Open |
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
and `RecipeDto.ingredients` cascades validation. The bulk endpoint uses
`List<@Valid RecipeDto>`. `ApiExceptionHandler` maps `MethodArgumentNotValidException`,
`HandlerMethodValidationException`, and `ConstraintViolationException` to `400`.
`spring-boot-starter-validation` stays on the classpath through the web starter.

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

**Evidence:** `Recipe.ingredients` is `@ManyToMany(cascade = CascadeType.ALL)`.
`CascadeType.ALL` includes `REMOVE`. Deleting a recipe cascades to every ingredient in
its set.

**Verified behavior (H2 smoke test):**

- Unshared ingredients are deleted together with the recipe.
- If an ingredient is referenced by another recipe, Hibernate deletes the recipe's join
  rows, then tries to delete the ingredient. The foreign key from `recipe_ingredients`
  rejects the delete, the request returns `500` with a constraint-violation message, and
  the transaction rolls back. The recipe remains.
- Deleting a recipe with an empty ingredient set succeeds.

**Impact:** recipes that share ingredients cannot be deleted at all, and the failure is
a 500 rather than a useful conflict response.

**Fix:** reduce the cascade to `PERSIST` and `MERGE`, or remove the cascade and persist
new ingredients explicitly in the service. Decide who owns an ingredient and add a
cleanup strategy for orphan rows. Consider returning `409 Conflict` when an ingredient
is still referenced, or removing only the join rows. This is a data-model change and
needs a migration plan.

### DEV-1 — No tests

**Evidence:** there was no `src/test` directory; `./mvnw test` reported "No tests to
run".

**Fix applied:** added 24 tests covering the two mappers, `RecipeRepository`, and the
full HTTP API. They run on H2 in PostgreSQL mode via the `test` profile and never touch
a remote database. Use `@SpringBootTest(RANDOM_PORT)` plus
`TestRestTemplate` for error responses, because `MockMvc` rethrows unhandled exceptions
instead of rendering the error body.

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

### CODE-1 — `StepRepository` is malformed and unused

**Evidence:** `StepRepository` extends the `org.springframework.stereotype.Repository`
annotation instead of `org.springframework.data.repository.Repository`, and declares
type parameters named `String` and `UUID` that shadow the real types. It compiles but
is not a Spring Data repository and is never injected.

**Fix:** delete the file. There is no `Step` entity; steps are an `@ElementCollection`
on `Recipe`. If a separate step entity is introduced, model it explicitly and extend
`JpaRepository<Step, UUID>`.

### DATA-2 — Ingredient triple has no unique constraint

**Evidence:** `findByNameIngredientAndAmountAndMeasurement` assumes uniqueness, but
there is no `@Table(uniqueConstraints = ...)` or database constraint. Duplicate rows
cause `IncorrectResultSizeDataAccessException`.

**Fix:** add a unique constraint on `(name_ingredient, amount, measurement)`, deduplicate
existing rows in a migration, and handle the constraint violation. Consider normalizing
ingredient names and units instead of using free text.

### DATA-3 — `ddl-auto: update` is the only schema management

**Evidence:** `spring.jpa.hibernate.ddl-auto=update`; no Flyway or Liquibase.

**Risks:** implicit changes, no rollback, no reviewable migrations, and destructive
changes are not applied. Not suitable for production.

**Fix:** add Flyway or Liquibase, write a baseline migration for the current schema,
then switch to `ddl-auto: validate`. Test migrations against a copy of real data.

### OPS-2 — Dockerfile uses a fragile wildcard copy

**Evidence:**

- `COPY --from=build "MediterraneApp/target/*.jar" app.jar` quotes a wildcard; Docker
  does not expand globs inside quotes.
- The build stage runs `mvn clean install` without skipping tests.
- No `HEALTHCHECK`, no non-root user.

**Fix:** copy the exact JAR or an unquoted glob, add a non-root user, and add a health
check. Build with the committed wrapper (`./mvnw`) for reproducibility. The provided
`.dockerignore` keeps `target/` and IDE state out of the build context.

### OPS-3 — No auth, CORS, actuator, or rate limiting

**Evidence:** no security starter, no CORS configuration, no actuator dependency.

**Risk:** anyone who can reach the port can read and mutate all data.

**Fix:** add authentication before exposing the API. Add CORS explicitly if a browser
client is added. Add actuator only with a minimal exposure set (`health`, `info`). Add
rate limiting at the gateway or with a filter.

### API-5 — No pagination on `GET /recipe`

**Evidence:** `recipeService.getAllRecipe()` calls `findAll()` and returns every row.

**Fix:** accept `page` and `size` (or cursor parameters), return a page envelope, and
document the change. This is a response-shape change; coordinate it with clients.

### API-6 — `POST /recipe/bulk` is not transactional

**Evidence:** the controller loops over `createRecipe` with no transaction. A failure
halfway leaves earlier items saved.

**Fix:** move the loop into a service method annotated `@Transactional` and return a
result that identifies failures, or fail the whole batch. Document the all-or-nothing
semantics.

### CFG-1 — Hibernate dialect is misspelled and misplaced

**Evidence:**

```yaml
spring:
  jpa:
    hibernate:
      dialect: org.hibernate.dialect.PostgreSQLDialec
```

The value is truncated, and `dialect` belongs under
`spring.jpa.properties.hibernate.dialect`. Hibernate currently auto-detects the
dialect, so the app still works.

**Fix:** remove the line (preferred with a single known database) or set:

```yaml
spring:
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

### DEV-5 — Mapper is constructed manually in the controller

**Evidence:** `RecipeController` does `this.recipeMapper = new RecipeMapper(...)`, while
`RecipeMapper` also has an `@Autowired` field that is ignored because the mapper is not
a Spring bean.

**Consequences:** the mapper cannot be replaced or mocked cleanly, and slice tests need
to work around it.

**Fix:** annotate `RecipeMapper` with `@Component`, inject it through the constructor,
and remove the `@Autowired` field. Do this together with tests.

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

### DEV-6 — `@Autowired` on final fields is redundant

**Evidence:** `RecipeController` annotates final fields that are assigned in the
constructor. Constructor injection already satisfies the dependency.

**Fix:** remove the annotations when touching the class. Do not perform a broad
formatting-only change.

### REPO-1 — `mediterraneapp.iml` was tracked

**Evidence:** the IntelliJ module file was committed even though `.gitignore` lists
`*.iml` (tracked files are not affected by ignore rules).

**Fix applied:** removed the file. `.gitignore` still ignores `*.iml`; regenerate it
locally if needed.

### REPO-2 — `HELP.md` is stale generated output

**Evidence:** Spring Initializr generated `HELP.md`, which points at old Spring Boot
documentation and adds nothing beyond [`README.md`](../README.md).

**Fix:** delete it, or regenerate it if the project is upgraded. Do not treat it as
project documentation.

### REPO-3 — Trailing-dot Maven `groupId`

**Evidence:** the POM uses `<groupId>com.ges.</groupId>` while the package is
`com.ges.mediterraneapp`. Maven accepts it, but the trailing dot is unusual and can
confuse tooling.

**Fix:** leave it unless you control every consumer. Changing it changes the published
coordinates and any dependency references.

## Verification notes

- Build verified with Temurin 25.0.5+8 and Maven Wrapper 3.9.16 (Spring Boot 4.1.2):
  `./mvnw -B -DskipTests package` → `BUILD SUCCESS`.
- `./mvnw test` → 24 tests, 0 failures, `BUILD SUCCESS`.
- Spring Boot 4 / Jackson 3 serialize enums with `toString()`, which would have changed
  the `"I_AM_A_TEAPOT"`, `"CREATED"`, and `"OK"` bodies. `JacksonConfig` registers an
  `HttpStatus` serializer that writes the enum name.
- No database credentials are committed; the datasource is supplied through
  `SPRING_DATASOURCE_*` at runtime.
- Jasypt and `@EnableEncryptableProperties` were removed; the build no longer carries the
  low-severity Jasypt advisory.
- The integration tests exercise the API against H2 in PostgreSQL mode (create, read,
  upsert, bulk, validation, missing row returning 404, invalid UUID returning 400, PUT
  with matching/omitted/mismatched UUID, delete, shared-ingredient delete). The same
  behavior is expected on PostgreSQL, but it was not exercised against a live PostgreSQL
  server.
