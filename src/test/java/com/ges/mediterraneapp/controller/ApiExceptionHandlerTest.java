package com.ges.mediterraneapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void dataIntegrityViolationReturns409WithoutLeakingDetails() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/recipe");

        ResponseEntity<ApiError> response = handler.handleConflict(
                new DataIntegrityViolationException("duplicate key value violates unique constraint"),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().error()).isEqualTo("Conflict");
        assertThat(response.getBody().message())
                .isEqualTo("Request conflicts with existing data")
                .doesNotContain("duplicate key value");
        assertThat(response.getBody().path()).isEqualTo("/recipe");
    }
}
