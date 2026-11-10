package com.ges.mediterraneapp;

import com.ges.mediterraneapp.model.dao.repository.IngredientRepository;
import com.ges.mediterraneapp.model.dao.repository.RecipeRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end HTTP tests against the real application context, backed by H2 in
 * PostgreSQL compatibility mode. A real servlet container is started
 * ({@code RANDOM_PORT}) so error responses go through Spring Boot's error dispatch,
 * exactly as they do in production.
 *
 * <p>Each test uses unique names so tests do not depend on execution order or on a
 * clean database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class RecipeApiIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Test
    void statusReturnsTeapotJsonString() {
        ResponseEntity<String> response = rest.getForEntity("/recipe/status", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertJson(response);
        assertThat(response.getBody()).isEqualTo("\"I_AM_A_TEAPOT\"");
    }

    @Test
    void createRecipeReturnsCreatedRecipe() {
        String name = unique("Tortilla");
        String ingredient = unique("Egg");
        String uuid = createRecipe(name, ingredient);

        ResponseEntity<String> response = rest.getForEntity("/recipe/{uuid}", String.class, uuid);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertJson(response);
        assertThat(JsonPath.<String>read(response.getBody(), "$.uuid")).isEqualTo(uuid);
        assertThat(JsonPath.<String>read(response.getBody(), "$.name")).isEqualTo(name);
        assertThat(JsonPath.<String>read(response.getBody(), "$.relations")).isEqualTo("test");
        assertThat(JsonPath.<List<String>>read(response.getBody(), "$.steps")).hasSize(2);
        assertThat(JsonPath.<List<String>>read(response.getBody(), "$.ingredients[*].nameIngredient"))
                .containsExactly(ingredient);
    }

    @Test
    void listRecipesContainsCreatedRecipe() {
        String uuid = createRecipe(unique("Paella"), unique("Rice"));

        ResponseEntity<String> response = rest.getForEntity("/recipe", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertJson(response);
        assertThat(JsonPath.<List<String>>read(response.getBody(), "$[*].uuid")).contains(uuid);
    }

    @Test
    void getMissingRecipeReturns404() {
        ResponseEntity<String> response = rest.getForEntity(
                "/recipe/{uuid}", String.class, UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertJson(response);
        assertThat(JsonPath.<Integer>read(response.getBody(), "$.status")).isEqualTo(404);
        assertThat(JsonPath.<String>read(response.getBody(), "$.message")).contains("Recipe not found");
    }

    @Test
    void getInvalidUuidReturns400() {
        ResponseEntity<String> response = rest.getForEntity(
                "/recipe/{uuid}", String.class, "not-a-uuid");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertJson(response);
        assertThat(JsonPath.<String>read(response.getBody(), "$.message"))
                .contains("Invalid UUID string");
    }

    @Test
    void bulkCreateReturnsCreatedAndPersistsEveryRecipe() {
        String firstName = unique("BulkOne");
        String secondName = unique("BulkTwo");
        String body = "[" + recipeJson(firstName, unique("BulkIngredientOne")) + ","
                + recipeJson(secondName, unique("BulkIngredientTwo")) + "]";

        ResponseEntity<String> response = postJson("/recipe/bulk", body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertJson(response);
        assertThat(response.getBody()).isEqualTo("\"CREATED\"");
        assertThat(recipeRepository.findByName(firstName)).isPresent();
        assertThat(recipeRepository.findByName(secondName)).isPresent();
    }

    @Test
    void postWithExistingNameReturns200AndUpdatesTheSameRecipe() {
        String name = unique("Upsert");

        ResponseEntity<String> first = postJson("/recipe",
                recipeJson(name, unique("UpsertIngredientOne")));
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String firstUuid = JsonPath.read(first.getBody(), "$.uuid");

        ResponseEntity<String> second = postJson("/recipe",
                recipeJson(name, unique("UpsertIngredientTwo")));
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        String secondUuid = JsonPath.read(second.getBody(), "$.uuid");

        assertThat(secondUuid).isEqualTo(firstUuid);
        assertThat(recipeRepository.findAll().stream()
                .filter(recipe -> name.equals(recipe.getName())))
                .hasSize(1);
    }

    @Test
    void createRecipeWithBlankNameReturns400() {
        ResponseEntity<String> response = postJson("/recipe", """
                {"name":"   ","ingredients":[]}
                """);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertJson(response);
        assertThat(JsonPath.<Integer>read(response.getBody(), "$.status")).isEqualTo(400);
        assertThat(JsonPath.<String>read(response.getBody(), "$.message")).contains("name");
    }

    @Test
    void createRecipeWithInvalidIngredientReturns400() {
        ResponseEntity<String> response = postJson("/recipe", """
                {"name":"%s","ingredients":[{"amount":"1","measurement":"kg"}]}
                """.formatted(unique("InvalidIngredient")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertJson(response);
        assertThat(JsonPath.<String>read(response.getBody(), "$.message"))
                .contains("nameIngredient");
    }

    @Test
    void createRecipeWithTooLongNameReturns400() {
        ResponseEntity<String> response = postJson("/recipe", """
                {"name":"%s","ingredients":[]}
                """.formatted("a".repeat(256)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertJson(response);
        assertThat(JsonPath.<String>read(response.getBody(), "$.message")).contains("name");
    }

    @Test
    void bulkCreateWithBlankNameReturns400() {
        ResponseEntity<String> response = postJson("/recipe/bulk", """
                [{"name":"   ","ingredients":[]}]
                """);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertJson(response);
        assertThat(JsonPath.<String>read(response.getBody(), "$.message")).contains("blank");
    }

    @Test
    void bulkCreateWithInvalidItemWritesNothing() {
        String validName = unique("BulkAtomicValid");
        String body = "[" + recipeJson(validName, unique("BulkAtomicIngredient")) + ","
                + """
                {"name":"   ","ingredients":[]}
                """ + "]";

        ResponseEntity<String> response = postJson("/recipe/bulk", body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertJson(response);
        assertThat(recipeRepository.findByName(validName)).isEmpty();
    }

    @Test
    void updateRecipeWithMatchingUuidChangesTheRow() {
        String uuid = createRecipe(unique("Before"), unique("UpdateIngredient"));
        String newName = unique("After");

        ResponseEntity<String> response = putJson("/recipe/{uuid}",
                """
                        {"uuid":"%s","name":"%s","steps":["only"]}
                        """.formatted(uuid, newName),
                uuid);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertJson(response);
        assertThat(JsonPath.<String>read(response.getBody(), "$.uuid")).isEqualTo(uuid);
        assertThat(JsonPath.<String>read(response.getBody(), "$.name")).isEqualTo(newName);
        assertThat(JsonPath.<List<String>>read(response.getBody(), "$.steps")).containsExactly("only");

        ResponseEntity<String> reloaded = rest.getForEntity("/recipe/{uuid}", String.class, uuid);
        assertThat(reloaded.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(JsonPath.<String>read(reloaded.getBody(), "$.name")).isEqualTo(newName);
    }

    @Test
    void updateRecipeWithoutBodyUuidUsesPathUuid() {
        String uuid = createRecipe(unique("NoBodyUuid"), unique("NoBodyUuidIngredient"));
        String newName = unique("NoBodyUuidAfter");

        ResponseEntity<String> response = putJson("/recipe/{uuid}",
                """
                        {"name":"%s","steps":["only"]}
                        """.formatted(newName),
                uuid);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertJson(response);
        assertThat(JsonPath.<String>read(response.getBody(), "$.uuid")).isEqualTo(uuid);
        assertThat(JsonPath.<String>read(response.getBody(), "$.name")).isEqualTo(newName);
    }

    @Test
    void updateRecipeWithMismatchedUuidReturns400AndKeepsTheRow() {
        String name = unique("Unchanged");
        String uuid = createRecipe(name, unique("MismatchIngredient"));

        ResponseEntity<String> response = putJson("/recipe/{uuid}",
                """
                        {"uuid":"%s","name":"Should not be saved"}
                        """.formatted(UUID.randomUUID()),
                uuid);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertJson(response);
        assertThat(JsonPath.<String>read(response.getBody(), "$.message"))
                .contains("match the path uuid");

        ResponseEntity<String> reloaded = rest.getForEntity("/recipe/{uuid}", String.class, uuid);
        assertThat(reloaded.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(JsonPath.<String>read(reloaded.getBody(), "$.name")).isEqualTo(name);
    }

    @Test
    void deleteRecipeReturnsOkAndRemovesTheRow() {
        String uuid = createRecipe(unique("DeleteMe"), unique("DeleteIngredient"));

        ResponseEntity<String> response = delete("/recipe/{uuid}", uuid);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertJson(response);
        assertThat(response.getBody()).isEqualTo("\"OK\"");

        ResponseEntity<String> reloaded = rest.getForEntity("/recipe/{uuid}", String.class, uuid);
        assertThat(reloaded.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(JsonPath.<String>read(reloaded.getBody(), "$.message")).contains("Recipe not found");
    }

    @Test
    void deleteMissingRecipeReturns404() {
        ResponseEntity<String> response = delete("/recipe/{uuid}", UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertJson(response);
        assertThat(JsonPath.<String>read(response.getBody(), "$.message")).contains("Recipe not found");
    }

    @Test
    void deleteRecipeRemovesUnsharedIngredients() {
        String uuid = createRecipe(unique("OrphanOwner"), unique("OrphanIngredient"));
        String ingredientUuid = readIngredientUuid(uuid);

        assertThat(ingredientRepository.findByUuid(UUID.fromString(ingredientUuid))).isPresent();

        ResponseEntity<String> response = delete("/recipe/{uuid}", uuid);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ingredientRepository.findByUuid(UUID.fromString(ingredientUuid))).isEmpty();
    }

    @Test
    void deleteRecipeWithSharedIngredientKeepsBothRecipesAndTheIngredient() {
        String sharedIngredient = unique("SharedSalt");
        String firstUuid = createRecipe(unique("SharedFirst"), sharedIngredient);
        String secondUuid = createRecipe(unique("SharedSecond"), sharedIngredient);
        String ingredientUuid = readIngredientUuid(firstUuid);

        ResponseEntity<String> response = delete("/recipe/{uuid}", firstUuid);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertJson(response);
        assertThat(response.getBody()).isEqualTo("\"OK\"");

        assertThat(rest.getForEntity("/recipe/{uuid}", String.class, firstUuid).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(rest.getForEntity("/recipe/{uuid}", String.class, secondUuid).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(ingredientRepository.findByUuid(UUID.fromString(ingredientUuid))).isPresent();
    }

    private String createRecipe(String name, String ingredientName) {
        ResponseEntity<String> response = postJson("/recipe", recipeJson(name, ingredientName));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertJson(response);
        return JsonPath.read(response.getBody(), "$.uuid");
    }

    private String readIngredientUuid(String recipeUuid) {
        ResponseEntity<String> response = rest.getForEntity("/recipe/{uuid}", String.class, recipeUuid);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return JsonPath.read(response.getBody(), "$.ingredients[0].uuid");
    }

    private ResponseEntity<String> postJson(String path, String body) {
        return rest.exchange(path, HttpMethod.POST, jsonEntity(body), String.class);
    }

    private ResponseEntity<String> putJson(String path, String body, Object... uriVariables) {
        return rest.exchange(path, HttpMethod.PUT, jsonEntity(body), String.class, uriVariables);
    }

    private ResponseEntity<String> delete(String path, Object... uriVariables) {
        return rest.exchange(path, HttpMethod.DELETE, null, String.class, uriVariables);
    }

    private static HttpEntity<String> jsonEntity(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private static void assertJson(ResponseEntity<String> response) {
        MediaType contentType = response.getHeaders().getContentType();
        assertThat(contentType)
                .as("content type of %s", response.getBody())
                .isNotNull();
        assertThat(contentType.isCompatibleWith(MediaType.APPLICATION_JSON))
                .as("expected JSON content type but was %s", contentType)
                .isTrue();
    }

    private static String recipeJson(String name, String ingredientName) {
        return """
                {
                  "name": "%s",
                  "relations": "test",
                  "preparedTime": "5m",
                  "cookedTime": "10m",
                  "totalTime": "15m",
                  "steps": ["one", "two"],
                  "ingredients": [
                    {"nameIngredient": "%s", "amount": "1", "measurement": "unit"}
                  ]
                }
                """.formatted(name, ingredientName);
    }

    private static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
