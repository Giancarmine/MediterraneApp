package com.ges.mediterraneapp;

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

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.cors.allowed-origins=https://client.example")
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class CorsConfigTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void preflightAllowsConfiguredOrigin() {
        ResponseEntity<String> response = rest.exchange("/recipe", HttpMethod.OPTIONS,
                preflight("https://client.example"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getAccessControlAllowOrigin())
                .isEqualTo("https://client.example");
    }

    @Test
    void preflightRejectsUnknownOrigin() {
        ResponseEntity<String> response = rest.exchange("/recipe", HttpMethod.OPTIONS,
                preflight("https://unknown.example"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private static HttpEntity<String> preflight(String origin) {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin(origin);
        headers.setAccessControlRequestMethod(HttpMethod.GET);
        return new HttpEntity<>(headers);
    }
}
