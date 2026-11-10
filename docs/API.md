# API reference

MediterraneApp exposes a small JSON API for recipes under `/recipe`. There is no
authentication, versioning, or ingredient endpoint.

- **Base URL (local):** `http://localhost:8080`
- **Content type:** `application/json`
- **Authentication:** none
- **Error format:** JSON with `timestamp`, `status`, `error`, `message`, and `path`.
  Handled exceptions use this shape; unhandled exceptions use Spring Boot's default
  error body, which has the same top-level keys.

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
- `404 Not Found` when it does not. `NoSuchElementException` is mapped by
  `ApiExceptionHandler`.
- `400 Bad Request` when `{uuid}` is not a valid UUID. `IllegalArgumentException` is
  mapped by the same handler.

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

Response: `201 Created` with the saved recipe on create, or `200 OK` with the saved
recipe when an existing `name` is updated. Both include the recipe `uuid`.

Behavior:

- The service looks up the recipe by `name` inside the request transaction.
- If a recipe with that name exists, the existing entity is updated and the response is
  `200 OK`.
- If no recipe exists, a new entity is created and the response is `201 Created`. A
  client-supplied `uuid` is accepted only in this new-name path.
- `name` is unique. A concurrent create that loses the race fails the unique constraint
  and returns `409 Conflict`.
- Ingredients are resolved by the exact triple
  `(nameIngredient, amount, measurement)`. A missing triple becomes a new ingredient
  and is persisted through the `PERSIST`/`MERGE` cascade.
- Scalar fields are copied unconditionally, so omitting one on an update sets it to
  `null`. `steps` and `ingredients` are only replaced when the field is present;
  omitting them keeps the existing collection.
- Validation: `name` must not be blank and at most 255 characters; each ingredient's
  `nameIngredient` must not be blank and at most 255 characters; `amount` and
  `measurement` are limited to 255 characters; each step is limited to 2500 characters.
  Violations return `400 Bad Request` before persistence. Syntactically invalid JSON is
  rejected earlier by Jackson with `400`.

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
`200` and the JSON body is the string `"CREATED"`. Request validation applies to every
item; an invalid item returns `400 Bad Request` before any item is processed. The whole
batch is one transaction: a persistence failure rolls back every item, so the endpoint is
all-or-nothing. A conflict with an existing unique value returns `409 Conflict`.

### `PUT /recipe/{uuid}`

Updates a recipe. The path UUID identifies the entity; the body `uuid` is optional.

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
- `404 Not Found` when the path UUID does not exist.
- `400 Bad Request` when the path UUID or a supplied body `uuid` is invalid.
- The path UUID is authoritative. The body `uuid` is optional; if present, it must match
  the path. A mismatch returns `400 Bad Request` and leaves the row unchanged.

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
- `404 Not Found` when the UUID does not exist.
- `400 Bad Request` when the UUID is invalid.
- Deleting a recipe removes the recipe and its join rows. Ingredients that are no longer
  referenced by any recipe are deleted; ingredients still referenced by another recipe
  are kept. A shared ingredient no longer blocks the delete or returns `500`.

## Ingredient API

There is no `IngredientController`. `IngredientService` and
`IngredientRepository` exist, but ingredients can only be created or referenced through
a recipe payload. Adding a public ingredient API is an additive change, not a refactor.

## Error responses

`ApiExceptionHandler` is a `@RestControllerAdvice` that maps:

- `NoSuchElementException` → `404 Not Found`
- `IllegalArgumentException` → `400 Bad Request`
- `MethodArgumentNotValidException`, `HandlerMethodValidationException`, and
  `ConstraintViolationException` → `400 Bad Request`
- `DataIntegrityViolationException` → `409 Conflict` with a generic message

A missing recipe produces:

```json
{
  "timestamp": "2026-11-09T14:10:23.026Z",
  "status": 404,
  "error": "Not Found",
  "message": "Recipe not found: 00000000-0000-0000-0000-000000000000",
  "path": "/recipe/00000000-0000-0000-0000-000000000000"
}
```

Unhandled exceptions still use Spring Boot's default error body, which has the same
top-level keys. A conflict response deliberately omits the database detail:

```json
{
  "timestamp": "2026-11-09T14:10:23.026Z",
  "status": 409,
  "error": "Conflict",
  "message": "Request conflicts with existing data",
  "path": "/recipe"
}
```

Do not build clients that depend on the exact `message` text.
`spring.web.error.include-message: always` in `application.yml` preserves the pre-upgrade
payload (Boot 2.2 included the message by default); remove it if leaking exception text
is a concern.

## Compatibility rules for agents

- Keep existing paths and JSON field names unless the owner approves a breaking change.
- Add new fields as optional and non-null-safe on read.
- The upsert-by-name behavior of `POST /recipe` is deliberate; changing the key needs a
  migration plan. `POST` returns `200` for an update and `201` for a create.
- `recipe.name` and the ingredient triple `(nameIngredient, amount, measurement)` are
  unique constraints. Existing duplicate rows must be cleaned up before those constraints
  can be applied.
- The path UUID is authoritative for `PUT /recipe/{uuid}`; a mismatched body UUID is a
  `400`.
- Keep the validation rules and their `400` responses in sync with this document.
- If you add an ingredient endpoint, document it here and update
  [`../AGENTS.md`](../AGENTS.md).
