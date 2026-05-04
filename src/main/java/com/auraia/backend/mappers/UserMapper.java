package com.auraia.backend.mappers;

import com.auraia.backend.models.dto.response.UserResponses;
import com.auraia.backend.models.entities.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponses.UserResponse toResponse(User user);

    UserResponses.AdminUserResponse toAdminResponse(User user);
}
