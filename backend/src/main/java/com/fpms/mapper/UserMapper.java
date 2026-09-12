package com.fpms.mapper;

import com.fpms.dto.request.RegisterRequest;
import com.fpms.dto.response.UserResponse;
import com.fpms.entity.User;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "authProvider", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    User toUser(RegisterRequest request);

    @Mapping(target = "role", source = "role.roleName")
    UserResponse toUserResponse(User user);
}
