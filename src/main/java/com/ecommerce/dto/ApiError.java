package com.ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/** Uniform error body returned by every failing request. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(LocalDateTime timestamp, int status, String error, String message,
                       String path, Map<String, String> fieldErrors) {
}
