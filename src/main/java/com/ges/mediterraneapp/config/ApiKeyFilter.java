package com.ges.mediterraneapp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Requires a shared API key on the recipe API.
 *
 * <p>The key comes from {@code app.api-key} ({@code APP_API_KEY}). Startup fails when it
 * is blank, so the API cannot be deployed unprotected by accident. The comparison is
 * constant-time to avoid leaking the key through response timing.
 *
 * <p>Open endpoints: actuator (platform probes), {@code GET /recipe/status} (legacy
 * health-style endpoint used by the Docker {@code HEALTHCHECK}), and CORS preflight
 * ({@code OPTIONS}) requests, which browsers send without custom headers.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-API-Key";

    private final byte[] apiKey;

    public ApiKeyFilter(@Value("${app.api-key:}") String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "app.api-key is not set; provide the API key through APP_API_KEY");
        }
        this.apiKey = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.startsWith("/actuator/") || path.equals("/recipe/status");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String supplied = request.getHeader(HEADER_NAME);
        if (supplied == null || !MessageDigest.isEqual(apiKey, supplied.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid API key");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
