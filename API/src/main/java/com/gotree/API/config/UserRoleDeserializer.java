package com.gotree.API.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.gotree.API.enums.UserRole;

import java.io.IOException;

public class UserRoleDeserializer extends JsonDeserializer<UserRole> {

    @Override
    public UserRole deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();

        if (value == null || value.isBlank()) {
            return UserRole.USER;
        }

        // Tenta com o valor como está (ex: "ADMIN", "USER")
        try {
            return UserRole.valueOf(value);
        } catch (IllegalArgumentException e) {
            // Se falhar, tenta remover "ROLE_" (ex: "ROLE_ADMIN" → "ADMIN")
            if (value.startsWith("ROLE_")) {
                return UserRole.valueOf(value.substring(5));
            }
            throw new IllegalArgumentException("Unknown role: " + value);
        }
    }
}