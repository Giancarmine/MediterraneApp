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
| SEC-1 | P0 | Credentials committed in `application.yml` | Open |
| OPS-1 | P0 | Configured PostgreSQL instance is retired | Open |
| API-1 | P1 | Missing recipe returns 500 instead of 404 | Open |
| API-2 | P1 | Validation is a no-op | Open |
| API-3 | P1 | `PUT` requires the body UUID to match the path | Open |
| API-4 | P1 | `POST` upserts by name and reports 201 for updates | Open |
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
| REPO-1 | P3 | `mediterraneapp.iml` was tracked | **Fixed** |
| REPO-2 | P3 | `HELP.md` is stale generated output | Open |
| REPO-3 | P3 | Trailing-dot Maven `groupId` | Open |

## P0 — security and data loss

### SEC-1 — Credentials committed in `application.yml`

**Evidence:** `src/main/resources/application.yml` contains a PostgreSQL host, a
database username, and a Jasypt `ENC(...)` password. The repository is public.

The password is encrypted, but the host and username are plaintext, and the encryption
key may be available in the deployment environment or in other committed files. Treat
the whole credential set as compromised.

**Fix:**

1. Rotate the database user and password now.
2. Replace the hard-coded values with environment variables or a local profile.
3. Keep only placeholders in `application.yml`, for example:

   ```yaml
   spring:
     datasource:
       url: ${SPRING_DATASOURCE_URL}
       username: ${SPRING_DATASOURCE_USERNAME}
       password: ${SPRING_DATASOURCE_PASSWORD}
   ```

4. Audit the Git history and any forks. Deleting the file in a new commit does not
   remove the old values.
5. If the Jasypt key was committed or shared, rotate it and re-encrypt the values.

Do not copy the current values into issues, pull requests, or documentation.

### OPS-1 — Configured PostgreSQL instance is retired

**Evidence:** `application.yml` points at an old Heroku PostgreSQL host. Heroku's free
PostgreSQL tier has been discontinued, so the host is expected to be unreachable.

**Symptom:** startup fails with a connection timeout or `UnknownHostException`.

**Fix:** provision a local or managed PostgreSQL database and supply
`SPRING_DATASOURCE_*`. See [`DEPLOYMENT.md`](DEPLOYMENT.md).

## P1 — blocks reliable use

### API-1 — Missing recipe returns 500 instead of 404

**Evidence:** `RecipeService.findRecipeByUuid` and `findIngredientByUuid` call
`orElseThrow()` with no exception mapping. Invalid UUID strings throw
`IllegalArgumentException`.

**Fix:** add one `@RestControllerAdvice`:

```java
@ExceptionHandler(NoSuchElementException.class)
ResponseEntity<Void> notFound() {
    return ResponseEntity.notFound().build();
}

@ExceptionHandler(IllegalArgumentException.class)
ResponseEntity<Void> badRequest() {
    return ResponseEntity.badRequest().build();
}
```

Add tests for both mappings and document the new responses in
[`API.md`](API.md).

### API-2 — Validation is a no-op

**Evidence:** `RecipeController` uses `@Valid`, but `RecipeDto` and `IngredientDto`
declare no constraints. Payloads with missing or empty fields reach Hibernate;
syntactically invalid JSON is rejected earlier by Jackson.

**Fix:** add Bean Validation annotations, for example `@NotBlank` on `name`,
`@NotEmpty` on `steps`, `@Valid` on the ingredient set, and constraints on ingredient
fields. For the bulk endpoint, use `List<@Valid RecipeDto>` so container element
validation runs. Map `MethodArgumentNotValidException` to `400` in the advice class.
Confirm `spring-boot-starter-validation` remains on the classpath (it is currently
transitive through the web starter).

### API-3 — `PUT` requires the body UUID to match the path

**Evidence:** `RecipeMapper.updateModel` always sets
`recipe.setUuid(recipeDto.getUuid() != null ? UUID.fromString(...) : null)`. The
controller loads the entity by the path UUID, then the mapper overwrites its identifier
with the body value.

**Verified behavior (H2 smoke test):**

- Body without `uuid` → `500`, message
  `identifier of an instance of ... was altered from <path-uuid> to null`. The row is
  unchanged.
- Body with a different UUID → `500`, message
  `identifier of an instance of ... was altered from <path-uuid> to <body-uuid>`. The
  row is unchanged.
- Body with the matching UUID → the intended update.

**Fix (preferred):** do not map the UUID in `updateModel`; keep the loaded entity's
identity. If the client sends a UUID, reject a mismatch with `400`. Document that the
path is authoritative. Add regression tests for all three cases.

### API-4 — `POST` upserts by name and returns 201 for updates

**Evidence:** `RecipeController.createRecipe` calls `findRecipeByName`; when a recipe
exists, it copies the existing UUID onto the DTO and saves. The response is always
`201 Created`.

**Consequences:** duplicate names silently overwrite data, and clients cannot
distinguish create from update.

**Fix:** choose an explicit contract:

- keep upsert but return `200` on update and `201` on create; or
- reject duplicates with `409 Conflict`; or
- move upsert to a separate endpoint.

Whichever option is chosen, update [`API.md`](API.md) and add tests.

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

**Fix applied:** added 20 tests covering the two mappers, `RecipeRepository`, and the
full HTTP API. They run on H2 in PostgreSQL mode via the `test` profile and never touch
the configured remote database. Use `@SpringBootTest(RANDOM_PORT)` plus
`TestRestTemplate` for error responses, because `MockMvc` rethrows unhandled exceptions
instead of rendering the 500 body.

### DEV-2 — No CI

**Evidence:** there was no `.github/workflows` directory.

**Fix applied:** `.github/workflows/ci.yml` runs `./mvnw -B verify` on Temurin 17 for
pushes and pull requests to `main`, caches the Maven repository, and uploads Surefire
reports when a run fails. Add dependency and container scanning once the build is
stable.

### DEV-4 — Jasypt dependency was unbuildable

**Evidence:** building the original POM failed while reading
`jasypt-spring-boot-starter:2.0.0`, because its POM imported
`spring-cloud-dependencies:Finchley.M8` from `repo.spring.io/libs-milestone`, which now
returns `401 Unauthorized`.

**Fix applied:** the dependency now tracks `jasypt-spring-boot-starter:4.0.4`, which is
the line that supports Spring Boot 4. `./mvnw -B -DskipTests package` succeeds on
Temurin 17.0.21.

Keep the Jasypt version current. If a future version changes property names or
encryption defaults, update this document and the configuration notes.

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

- Build verified with Temurin 17.0.21 and Maven Wrapper 3.9.16 (Spring Boot 4.1.2):
  `./mvnw -B -DskipTests package` → `BUILD SUCCESS`.
- `./mvnw test` → 20 tests, 0 failures, `BUILD SUCCESS`.
- Spring Boot 4 / Jackson 3 serialize enums with `toString()`, which would have changed
  the `"I_AM_A_TEAPOT"`, `"CREATED"`, and `"OK"` bodies. `JacksonConfig` registers an
  `HttpStatus` serializer that writes the enum name.
- The integration tests exercise the API against H2 in PostgreSQL mode (create, read,
  upsert, bulk, missing row, invalid UUID, PUT UUID mismatch, delete, shared-ingredient
  delete). The same behavior is expected on PostgreSQL, but it was not exercised against
  a live PostgreSQL server.
