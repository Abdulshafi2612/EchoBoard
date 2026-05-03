package com.echoboard.mapper;

import com.echoboard.dto.auth.RegisterRequest;
import com.echoboard.dto.auth.RegisterResponse;
import com.echoboard.dto.user.UserProfileResponse;
import com.echoboard.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User RegisterRequestToUser(RegisterRequest request);

    @Mapping(target = "message", ignore = true)
    RegisterResponse userToRegisterResponse(User user);

    UserProfileResponse userToUserProfileResponse(User user);
}