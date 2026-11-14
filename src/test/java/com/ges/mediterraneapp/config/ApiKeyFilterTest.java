package com.ges.mediterraneapp.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiKeyFilterTest {

    private final ApiKeyFilter filter = new ApiKeyFilter("secret-key");

    @Test
    void missingKeyReturns401AndStopsTheChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/recipe");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getErrorMessage()).isEqualTo("Missing or invalid API key");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void wrongKeyReturns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/recipe");
        request.addHeader(ApiKeyFilter.HEADER_NAME, "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void correctKeyPassesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/recipe");
        request.addHeader(ApiKeyFilter.HEADER_NAME, "secret-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void preflightBypassesAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/recipe");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void actuatorBypassesAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void statusBypassesAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/recipe/status");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void blankKeyFailsFast() {
        assertThatThrownBy(() -> new ApiKeyFilter(" "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_API_KEY");
    }
}
