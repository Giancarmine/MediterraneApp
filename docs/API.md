# API reference

MediterraneApp exposes a small JSON API for recipes under `/recipe`. There is no
authentication, versioning, or ingredient endpoint.

- **Base URL (local):** `http://localhost:8080`
- **Content type:** `application/json`
- **Authentication:** none
- **Error format:** Spring Boot's default error JSON, with `message` enabled and an
  ISO-8601 `timestamp`

This document describes the code as it exists, including its quirks. See
[`KNOWN_ISSUES.md`](KNOWN_ISSUES.md) before relying on any behavior in production.

## Recipe JSON shape

```json
{
  "uuid": "3f2b1c4e-8a7d-4c2b-9e61-5d0f4a6b7c81",
  "name": "Tortilla de patatas",
  "relations": "Spanish",
  "preparedTime": "15m",
  "cookedTime": "30m",
  "totalTime": "45m",
  "steps": [
    "Peel and slice the potatoes.",
    "Cook the potatoes and onion slowly in olive oil.",
    "Beat the eggs, mix, and cook the tortilla on both sides."
  ],
  "ingredients": [
    {
      "uuid": "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d",
      "amount": "1",
      "measurement": "kg",
      "nameIngredient": "potato"
    },
    {
      "uuid": "b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e",
      "amount": "6",
      "measurement": "unit",
      "nameIngredient": "egg"
    }
  ]
}
```

Notes:

- `uuid` is a string in JSON and a `java.util.UUID` in the entity.
- `preparedTime`, `cookedTime`, `totalTime`, `amount`, and `measurement` are free-text
  strings, not numeric or duration types.
- `relations` is an undocumented free-text field.
- Null fields are omitted because the DTOs use `@JsonInclude(NON_NULL)`.
- `steps` is a set, so order is not guaranteed by the API contract.
- `ingredients` is a set, so order is not guaranteed.

## Endpoints

### `GET /recipe/status`

Health-style endpoint. It does not check the database.

```bash
curl -i http://localhost:8080/recipe/status
```

```http
HTTP/1.1 200
Content-Type: application/json

"I_AM_A_TEAPOT"
```

The method returns `HttpStatus.I_AM_A_TEAPOT` from a `@RestController`, so the response
body is the JSON string `"I_AM_A_TEAPOT"`. It does **not** return HTTP 418.
`JacksonConfig` registers an `HttpStatus` serializer that writes `name()`, because
Jackson 3 would otherwise use `toString()` and produce `"418 I_AM_A_TEAPOT"`.

### `GET /recipe`

Returns every recipe in the database. No pagination or filtering.

```bash
curl -s http://localhost:8080/recipe
```

```json
[
  {
    "uuid": "3f2b1c4e-8a7d-4c2b-9e61-5d0f4a6b7c81",
    "name": "Tortilla de patatas",
    "preparedTime": "15m",
    "cookedTime": "30m",
    "totalTime": "45m",
    "steps": ["Peel and slice the potatoes."],
    "ingredients": []
  }
]
```

Returns `200 OK` with `[]` when the table is empty.

### `GET /recipe/{uuid}`

Returns one recipe.

```bash
curl -s http://localhost:8080/recipe/3f2b1c4e-8a7d-4c2b-9e61-5d0f4a6b7c81
```

- `200 OK` with the recipe when it exists.
- `500 Internal Server Error` when it does not. `RecipeService.findRecipeByUuid` uses
  `orElseThrow()` with no mapping to `404`.
- `500 Internal Server Error` when `{uuid}` is not a valid UUID.

### `POST /recipe`

Creates a recipe or updates an existing recipe with the same `name`.

```bash
curl -i -X POST http://localhost:8080/recipe \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Tortilla de patatas",
    "relations": "Spanish",
    "preparedTime": "15m",
    "cookedTime": "30m",
    "totalTime": "45m",
    "steps": [
      "Peel and slice the potatoes.",
      "Cook the potatoes and onion slowly in olive oil.",
      "Beat the eggs, mix, and cook the tortilla on both sides."
    ],
    "ingredients": [
      { "nameIngredient": "potato", "amount": "1", "measurement": "kg" },
      { "nameIngredient": "egg", "amount": "6", "measurement": "unit" }
    ]
  }'
```

Response: `201 Created` with the saved recipe, including its `uuid`.

Behavior:

- The controller calls `RecipeService.findRecipeByName`.
- If a recipe with that name exists, its `uuid` is copied into the request DTO and the
  entity is updated. The response is still `201 Created`.
- If no recipe exists, a new entity is created. A client-supplied `uuid` is accepted
  only in this new-name path, because the mapper copies it onto the entity.
- Ingredients are resolved by the exact triple
  `(nameIngredient, amount, measurement)`. A missing triple becomes a new ingredient
  and is persisted through `CascadeType.ALL`.
- Scalar fields are copied unconditionally, so omitting one on an update sets it to
  `null`. `steps` and `ingredients` are only replaced when the field is present;
  omitting them keeps the existing collection.
- `@Valid` is declared but the DTO has no constraints, so payloads with missing or
  empty fields are accepted until they fail at the persistence layer. Syntactically
  invalid JSON is rejected earlier by Jackson with `400`.

### `POST /recipe/bulk`

Accepts a JSON array of recipe objects, using the same semantics as `POST /recipe` for
each item.

```bash
curl -i -X POST http://localhost:8080/recipe/bulk \
  -H 'Content-Type: application/json' \
  -d '[
    { "name": "Gazpacho", "preparedTime": "20m", "ingredients": [] },
    { "name": "Paella", "preparedTime": "30m", "ingredients": [] }
  ]'
```

```http
HTTP/1.1 200
Content-Type: application/json

"CREATED"
```

The method returns `HttpStatus.CREATED` from a `@RestController`, so the status stays
`200` and the JSON body is the string `"CREATED"`. There is no transaction around the
loop: if item 3 fails, items 1 and 2 remain saved.

### `PUT /recipe/{uuid}`

Updates a recipe. The path UUID identifies the entity; the body should repeat it.

```bash
curl -i -X PUT http://localhost:8080/recipe/3f2b1c4e-8a7d-4c2b-9e61-5d0f4a6b7c81 \
  -H 'Content-Type: application/json' \
  -d '{
    "uuid": "3f2b1c4e-8a7d-4c2b-9e61-5d0f4a6b7c81",
    "name": "Tortilla de patatas",
    "preparedTime": "15m",
    "cookedTime": "35m",
    "totalTime": "50m",
    "steps": ["Peel and slice the potatoes."],
    "ingredients": [
      { "nameIngredient": "potato", "amount": "1", "measurement": "kg" }
    ]
  }'
```

- `200 OK` with the updated recipe.
- `500 Internal Server Error` when the path UUID does not exist.
- `500 Internal Server Error` when the body omits `uuid` or sends a different one.
  `RecipeMapper.updateModel` rewrites the managed entity's identifier, and Hibernate
  throws `HibernateException: identifier of an instance of ... was altered`. The row is
  not changed. **Always send the path UUID in the body.**

### `DELETE /recipe/{uuid}`

```bash
curl -i -X DELETE http://localhost:8080/recipe/3f2b1c4e-8a7d-4c2b-9e61-5d0f4a6b7c81
```

```http
HTTP/1.1 200
Content-Type: application/json

"OK"
```

- `200 OK` with `"OK"` when the recipe is deleted.
- `500 Internal Server Error` when the UUID does not exist.
- Because the ingredient relation uses `CascadeType.ALL`, deleting a recipe also
  deletes its unshared ingredient rows.
- If any ingredient is shared with another recipe, the foreign key blocks the cascade:
  the request returns `500` with a constraint-violation message and the transaction
  rolls back, so the recipe remains. Verified against PostgreSQL-compatible H2.

## Ingredient API

There is no `IngredientController`. `IngredientService` and
`IngredientRepository` exist, but ingredients can only be created or referenced through
a recipe payload. Adding a public ingredient API is an additive change, not a refactor.

## Error responses

The application has no `@RestControllerAdvice`. Errors fall through to Spring Boot's
default error handling. A missing recipe typically produces:

```json
{
  "timestamp": "2026-11-09T14:10:23.026Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "No value present",
  "path": "/recipe/00000000-0000-0000-0000-000000000000"
}
```

Do not build clients that depend on the exact `message` text.
`spring.web.error.include-message: always` in `application.yml` preserves the pre-upgrade
payload (Boot 2.2 included the message by default); remove it if leaking exception text
is a concern. If you add error handling, map `NoSuchElementException` to `404` and
`IllegalArgumentException` to `400` in one `@RestControllerAdvice`.

## Compatibility rules for agents

- Keep existing paths and JSON field names unless the owner approves a breaking change.
- Add new fields as optional and non-null-safe on read.
- Do not change the upsert-by-name behavior of `POST /recipe` or the UUID semantics of
  `PUT /recipe/{uuid}` without documenting the migration.
- If you add validation, document the new `400` responses here.
- If you add an ingredient endpoint, document it here and update
  [`../AGENTS.md`](../AGENTS.md).
