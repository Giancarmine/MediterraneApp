package com.ges.mediterraneapp;

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
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the API-key gate through the real servlet container, without the
 * header-adding customizer used by {@link RecipeApiIntegrationTest}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class ApiKeyTest {

    private static final String VALID_KEY = "test-api-key";

    @Autowired
    private TestRestTemplate rest;

    @Test
    void recipeEndpointsRequireTheApiKey() {
        ResponseEntity<String> response = rest.getForEntity("/recipe", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(JsonPath.<Integer>read(response.getBody(), "$.status")).isEqualTo(401);
    }

    @Test
    void wrongApiKeyIsRejected() {
        ResponseEntity<String> response = get("/recipe", "wrong-key");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void validApiKeyIsAccepted() {
        ResponseEntity<String> response = get("/recipe", VALID_KEY);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void actuatorHealthStaysOpen() {
        ResponseEntity<String> response = rest.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(JsonPath.<String>read(response.getBody(), "$.status")).isEqualTo("UP");
    }

    @Test
    void statusEndpointStaysOpen() {
        ResponseEntity<String> response = rest.getForEntity("/recipe/status", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("\"I_AM_A_TEAPOT\"");
    }

    private ResponseEntity<String> get(String path, String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }
}
