package com.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDto {

    private AuthDto() {
    }

    public record RegisterRequest(
            @NotBlank(message = "Name is required")
            @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
            String name,

            @NotBlank(message = "Email is required")
            @Email(message = "Email must be valid")
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 64, message = "Password must be 8-64 characters")
            String password,

            @Pattern(regexp = "^$|^[0-9+\\- ]{7,20}$", message = "Phone number is invalid")
            String phone) {
    }

    public record LoginRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Email must be valid")
            String email,

            @NotBlank(message = "Password is required")
            String password) {
    }

    public record AuthResponse(String token, String tokenType, long expiresInMs, UserDto.UserResponse user) {
    }
}
