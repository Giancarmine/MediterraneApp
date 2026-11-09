package com.ges.mediterraneapp.controller;

import java.time.OffsetDateTime;

/**
 * Error body kept compatible with Spring Boot's default error JSON: the same
 * {@code timestamp}, {@code status}, {@code error}, {@code message}, and {@code path}
 * fields, but produced consistently for handled exceptions.
 */
record ApiError(OffsetDateTime timestamp, int status, String error, String message, String path) {
}
