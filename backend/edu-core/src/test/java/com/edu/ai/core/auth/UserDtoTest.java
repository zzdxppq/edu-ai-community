package com.edu.ai.core.auth;

import com.edu.ai.common.constant.UserRole;
import com.edu.ai.common.constant.UserType;
import com.edu.ai.core.auth.dto.UserDto;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the UserDto whitelist (Architect Round-2 L3 + Dev Notes §敏感数据):
 * {@code passwordHash}, {@code lastLoginAt}, {@code region}, {@code school}
 * must NEVER appear in the serialized response.
 */
class UserDtoTest {

    private static final Set<String> WHITELIST =
            Set.of("id", "phone", "username", "role", "userType", "createdAt");

    @Test
    void test_1_2_UNIT_011_userDtoNoSensitiveFields() throws Exception {
        UserDto dto = new UserDto(
                UUID.randomUUID(),
                "13800138000",
                "张三",
                UserRole.MEMBER_RURAL,
                UserType.USER,
                OffsetDateTime.now()
        );

        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        String json = mapper.writeValueAsString(dto);
        JavaType mapType = mapper.getTypeFactory().constructMapType(
                java.util.HashMap.class, String.class, Object.class);
        Map<String, Object> parsed = mapper.readValue(json, mapType);

        assertThat(parsed.keySet()).isSubsetOf(WHITELIST);
        assertThat(parsed).doesNotContainKey("passwordHash");
        assertThat(parsed).doesNotContainKey("lastLoginAt");
        assertThat(parsed).doesNotContainKey("region");
        assertThat(parsed).doesNotContainKey("school");
    }

    @Test
    void test_1_2_BLIND_DATA_004_userDtoReflectiveFieldWhitelist() {
        Set<String> declared = Arrays.stream(UserDto.class.getDeclaredFields())
                .filter(f -> !f.isSynthetic())
                .map(Field::getName)
                .collect(Collectors.toSet());

        assertThat(declared).isSubsetOf(WHITELIST);
    }
}
