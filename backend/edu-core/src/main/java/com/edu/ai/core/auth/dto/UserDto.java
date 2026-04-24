package com.edu.ai.core.auth.dto;

import com.edu.ai.common.constant.UserRole;
import com.edu.ai.common.constant.UserType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * User response DTO. Field set is the whitelist of user-safe data.
 * Deliberately omits {@code passwordHash}, {@code lastLoginAt},
 * {@code region}, {@code school} (Architect L3 — minimal disclosure).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private UUID id;
    private String phone;
    private String username;
    private UserRole role;
    private UserType userType;
    private OffsetDateTime createdAt;
}
