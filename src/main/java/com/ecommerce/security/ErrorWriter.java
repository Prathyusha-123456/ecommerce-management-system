package com.ecommerce.security;

import com.ecommerce.dto.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.LocalDateTime;

/** Writes the standard JSON error body for failures that happen inside the security filter chain. */
final class ErrorWriter {

    private ErrorWriter() {
    }

    static void write(HttpServletResponse response, ObjectMapper mapper, HttpStatus status,
                      String message, String path) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError body = new ApiError(LocalDateTime.now(), status.value(), status.getReasonPhrase(),
                message, path, null);
        mapper.writeValue(response.getOutputStream(), body);
    }
}
