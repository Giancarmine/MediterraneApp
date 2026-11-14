package com.ges.mediterraneapp.model.dto;

import java.util.List;

/**
 * Stable page envelope. Avoids serializing Spring Data's {@code Page} directly, whose
 * JSON shape is not part of its public contract.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
