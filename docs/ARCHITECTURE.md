# Architecture

MediterraneApp is a single-module Spring Boot application. It exposes a JSON API for
recipes, persists recipes and ingredients in PostgreSQL, and has no frontend.

## Runtime shape

```text
┌──────────────────────────────────────────────────────────────┐
│ Spring Boot 4.1.2 (embedded Tomcat 11, port 8080)            │
│                                                              │
│  RecipeController                                            │
│    │  RecipeDto / IngredientDto                              │
│    ▼                                                         │
│  RecipeService ──────► IngredientService                     │
│    │                        │                                │
│    ▼                        ▼                                │
│  RecipeRepository      IngredientRepository                  │
│    │                        │                                │
│    └──────────┬─────────────┘                                │
│               ▼                                              │
│         Hibernate 7.4 / HikariCP                             │
└───────────────┬──────────────────────────────────────────────┘
                ▼
          PostgreSQL
```

- **Entry point:** `com.ges.mediterraneapp.MediterraneappApplication`.
- **Web layer:** one `@RestController`, `RecipeController`.
- **Service layer:** concrete classes, no interfaces.
- **Persistence:** Spring Data JPA repositories over Hibernate.
- **Mapping:** hand-written classes, not MapStruct and not ModelMapper.
- **Configuration:** `src/main/resources/application.yml`; no profiles.

## Package map

```text
src/main/java/com/ges/mediterraneapp/
├── MediterraneappApplication.java     @SpringBootApplication
├── controller/
│   ├── RecipeController.java          /recipe endpoints
│   ├── ApiExceptionHandler.java       exception -> 400/404 mapping
│   └── ApiError.java                  error response body
├── service/
│   ├── RecipeService.java             recipe use cases
│   └── IngredientService.java         ingredient use cases
├── mapper/
│   ├── RecipeMapper.java              Recipe <-> RecipeDto
│   └── IngredientMapper.java          Ingredient <-> IngredientDto
└── model/
    ├── dao/
    │   ├── Recipe.java                JPA entity
    │   ├── Ingredient.java            JPA entity
    │   └── repository/
    │       ├── RecipeRepository.java
    │       └── IngredientRepository.java
    └── dto/
        ├── RecipeDto.java
        └── IngredientDto.java
```

## Class responsibilities

| Class | Responsibility | Notes |
| --- | --- | --- |
| `RecipeController` | HTTP routing, request validation, response status | Constructor-injected `RecipeService` and `RecipeMapper`. |
| `RecipeService` | Find, create, update, delete recipes | `upsertRecipe`, `upsertRecipes`, `updateRecipe`, and `deleteRecipe` are `@Transactional`; `getRecipes` is a paginated `@Transactional(readOnly = true)` read. Throws `NoSuchElementException` on missing rows; mapped to `404`. |
| `ApiExceptionHandler` | Maps handled exceptions to `400`/`404`/`409` JSON errors | One `@RestControllerAdvice`; keeps the default error field names. |
| `IngredientService` | Find, create, update, delete ingredients | Not exposed through HTTP. |
| `RecipeMapper` | Entity/DTO conversion and ingredient resolution | Spring `@Component`; calls `IngredientService` for each DTO ingredient. |
| `IngredientMapper` | Entity/DTO conversion | `toDto` is package-private. |
| `RecipeRepository` | `findByUuid`, `findByName`, `existsByIngredients_Uuid` | Derived queries. |
| `IngredientRepository` | `findByUuid`, `findByNameIngredientAndAmountAndMeasurement` | The triple is unique in the schema. |
| `Recipe` | Persistence model | Unidirectional many-to-many to ingredients; element collection for steps; unique `name`. |
| `Ingredient` | Persistence model | No back-reference to recipes. |
| `RecipeDto` / `IngredientDto` | JSON contract | `@JsonInclude(NON_NULL)`, string UUIDs. |
| `JacksonConfig` | JSON customization | Registers an `HttpStatus` serializer that writes `name()`, preserving the historical bodies. |
| `CorsConfig` | Opt-in CORS | Adds `/recipe/**` mappings only when `app.cors.allowed-origins` is non-empty. |

## Request flows

### Read one recipe

1. Tomcat dispatches `GET /recipe/{uuid}` to `RecipeController.read`.
2. The controller calls `RecipeService.findRecipeByUuid(uuid)`.
3. The service converts the string to `UUID` and calls
   `RecipeRepository.findByUuid`.
4. `orElseThrow()` raises `NoSuchElementException` when absent; `ApiExceptionHandler`
   maps it to `404 Not Found`. An invalid UUID becomes `400 Bad Request`.
5. `RecipeMapper.toDto` copies scalar fields, steps, and the ingredient set.
6. Spring serializes the DTO as JSON.

There is no transaction annotation. Each repository call runs in its own transaction.

### Create or update a recipe

1. Jackson 3 deserializes the body into `RecipeDto`; `@Valid` enforces the constraints
   before the service is called.
2. `RecipeService.upsertRecipe` runs in a transaction.
3. `doUpsert` looks the recipe up by `name`:
   - a miss creates a new entity;
   - a hit returns the existing entity.
4. The identifier is always server-generated. A client-supplied UUID is ignored on
   create; the generated value is returned in the response.
5. `RecipeMapper.updateModel` never rewrites the entity UUID:
   - copies every scalar field, including nulls;
   - replaces `steps` only when the request field is non-null;
   - for each `IngredientDto`, calls
     `IngredientService.findIngredientByNameAndAmountAndMeasurement`;
   - a miss returns a new transient `Ingredient(name, amount, measurement)`.
6. `save` persists the recipe. The `PERSIST`/`MERGE` cascade persists new ingredients and
   the join rows.
7. The saved entity is mapped back to a DTO and returned with `201 Created` on create or
   `200 OK` on update.

Consequences:

- `POST` is an upsert keyed by `name`.
- `@NotBlank` and `@Size` on the DTOs make `@Valid` effective; violations return `400`
  through `ApiExceptionHandler`.
- `recipe.name` is unique. Concurrent creates that race on the same name fail the
  constraint; `ApiExceptionHandler` maps `DataIntegrityViolationException` to a generic
  `409 Conflict`.
- Updating a recipe also removes ingredients that the previous version referenced and
  that no recipe references any more. An omitted `ingredients` field keeps the old set,
  so the cleanup only runs when the request replaces it.
- `upsertRecipes` handles the whole bulk body in one transaction. Validation still runs
  first, so an invalid item fails the request before any write; a persistence failure
  rolls back every item.

### Delete a recipe

1. `RecipeService.deleteRecipe` runs in a transaction and loads the entity.
2. It snapshots the ingredient set, deletes the recipe, and flushes so the join rows are
   gone.
3. Every snapshotted ingredient that no remaining recipe references
   (`existsByIngredients_Uuid`) is deleted.
4. Ingredients still referenced by another recipe are left in place. The request returns
   `200 OK` with `"OK"` in all cases, and `404 Not Found` when the recipe is absent.

The cascade is `PERSIST`/`MERGE` only, so deleting a recipe never deletes a shared
ingredient. The trade-off is that the orphan cleanup is explicit rather than a database
cascade. Updates use the same helper: ingredients dropped by a new ingredient set are
deleted when no recipe references them.

## Persistence model

```text
recipe
├── uuid           UUID, PK, generated
├── name           varchar
├── relations      varchar
├── prepared_time  varchar
├── cooked_time    varchar
├── total_time     varchar
├── created_at     timestamp
└── updated_at     timestamp

recipe_steps (Hibernate-generated name)
├── recipe_uuid    FK -> recipe
└── steps          varchar(2500)

ingredient
├── uuid            UUID, PK, generated
├── amount          varchar
├── measurement     varchar
├── name_ingredient varchar
├── created_at      timestamp
└── updated_at      timestamp

recipe_ingredients (Hibernate-generated name)
├── recipe_uuid       FK -> recipe
└── ingredients_uuid  FK -> ingredient
```

Details:

- The schema is created by Flyway from `src/main/resources/db/migration`. `V1__baseline.sql`
  matches the current entities, and Hibernate runs with `ddl-auto: validate` so entity
  and schema drift fails startup instead of being silently altered.
- Existing databases that were created by Hibernate `ddl-auto: update` are baselined at
  V1 (`baseline-on-migrate: true`, `baseline-version: 1`) without re-running the script.
  Their schema must already match V1, including the unique constraints.
- `Recipe.steps` is an `@ElementCollection` of `String` values with
  `@Column(length = 2500)`.
- `Recipe.ingredients` is a unidirectional `@ManyToMany(cascade = { PERSIST, MERGE })`.
  `Ingredient` has no `mappedBy` side. `REMOVE` is intentionally absent; deletion
  cleans up join rows and orphan ingredients explicitly.
- `createdAt` and `updatedAt` use Hibernate's `@CreationTimestamp` and
  `@UpdateTimestamp`. `AuditingEntityListener` is declared, but `@EnableJpaAuditing` is
  not present and is not needed for these annotations.
- Unique constraints: `uk_recipe_name` on `recipe.name`, and `uk_ingredient_identity` on
  `(name_ingredient, amount, measurement)`. Existing duplicate rows must be cleaned up
  before V1 is applied to a database that does not already have the constraints.
- `GET /recipe` uses a `PageRequest` (`page`, `size`, sorted by `name`) and maps the
  slice to DTOs inside a read-only transaction. `Recipe.steps` and
  `Recipe.ingredients` use `@BatchSize` so a page does not issue a query per recipe.

## Configuration and startup

`application.yml` sets:

- Hikari connection timeout (20 s) and maximum pool size (5);
- no datasource credentials; the URL, username, and password come from
  `SPRING_DATASOURCE_*` at runtime;
- `show-sql: true`;
- `ddl-auto: validate`, with Flyway owning all schema changes;
- Flyway is enabled with `baseline-on-migrate: true` and `baseline-version: 1`;
- `management.endpoints.web.exposure.include: health,info`, with
  `management.endpoint.health.show-details: never`;
- `app.cors.allowed-origins`, bound from `APP_CORS_ALLOWED_ORIGINS` and empty by default.

`CorsConfig` reads the origin list, trims it, drops blanks, and registers no mapping when
the result is empty. Actuator uses the same port as the API; there is no separate
management port.

There is no configuration-encryption layer. Jasypt was removed because no `ENC(...)`
values are configured and the datasource is supplied through the environment.

At startup Spring Boot creates the datasource from `SPRING_DATASOURCE_*`, Flyway applies
any pending migrations, Hibernate validates the resulting schema, and Tomcat binds port
8080. If the datasource variables are missing, the database is unreachable, a migration
fails, or the entity model does not match the schema, startup fails; there is no graceful
degradation.

## Design decisions and their trade-offs

| Decision | Consequence |
| --- | --- |
| DTO/entity separation | The JSON contract is independent of the schema, but requires manual mapping. |
| Hand-written mappers | No annotation processor or magic; more code to maintain. History shows MapStruct was tried and removed. |
| UUID primary keys | Safe for distributed creation and public URLs; slightly larger indexes. |
| `POST` upsert by name | Simple client workflow, but ambiguous names collide. Returns `201` on create and `200` on update. A unique constraint turns the race into a `409`. |
| `PERSIST`/`MERGE` cascade on ingredients | Ingredient rows are never silently deleted as a side effect; deletion must clean up orphans explicitly. |
| Flyway with Hibernate `ddl-auto: validate` | Schema changes are explicit, reviewable, and fail fast on drift; adds a migration step to every schema change. |
| No service interfaces | Less ceremony for a small app; harder to swap implementations. |
| Mapper as a Spring bean | Constructor injection and testability; mapper and service form the only cyclic-free collaboration. |
| Global exception handler | `ApiExceptionHandler` maps handled exceptions to `400`/`404`/`409`; unexpected failures still surface as `500`. |
| Paginated list with a fixed envelope | Bounds the response size and keeps the JSON shape stable; changes `GET /recipe` from an array to `PageResponse`. |

## Extension points

Safe, additive work:

- Add `IngredientController` over the existing `IngredientService`.
- Add filtering or search parameters to `GET /recipe` (document the new parameters).
- Broaden actuator exposure deliberately; keep `health`/`info` as the baseline.

Changes that need an explicit decision:

- Upgrading or downgrading Spring Boot (the code uses the `jakarta.*` namespace).
- Changing the upsert key from `name` to `uuid`.
- Changing the ingredient ownership or normalization strategy (for example, introducing
  a canonical ingredient table instead of the free-text triple).
- Adding authentication or a browser frontend.
- Moving to a multi-module build.
