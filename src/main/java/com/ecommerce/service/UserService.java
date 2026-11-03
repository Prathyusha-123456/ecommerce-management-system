package com.ecommerce.service;

import com.ecommerce.dto.UserDto.UpdateProfileRequest;
import com.ecommerce.dto.UserDto.UserResponse;
import com.ecommerce.entity.User;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.mapper.Mappers;
import com.ecommerce.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return Mappers.toUser(find(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = find(userId);
        user.setName(request.name().trim());
        user.setPhone(request.phone());
        user.setAddress(request.address());
        return Mappers.toUser(user); // managed entity: changes are flushed automatically on commit
    }

    private User find(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));
    }
}
