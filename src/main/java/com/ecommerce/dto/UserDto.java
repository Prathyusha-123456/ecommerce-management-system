package com.ecommerce.dto;

import com.ecommerce.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class UserDto {

    private UserDto() {
    }

    public record UserResponse(Long id, String name, String email, String phone, String address,
                               Role role, LocalDateTime createdAt) {
    }

    public record UpdateProfileRequest(
            @NotBlank(message = "Name is required")
            @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
            String name,

            @Pattern(regexp = "^$|^[0-9+\\- ]{7,20}$", message = "Phone number is invalid")
            String phone,

            @Size(max = 500, message = "Address must be at most 500 characters")
            String address) {
    }
}
