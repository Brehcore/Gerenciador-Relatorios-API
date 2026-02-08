package com.gotree.API.config;

import com.gotree.API.enums.UserRole;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class UserRoleConverter implements AttributeConverter<UserRole, String> {

    @Override
    public String convertToDatabaseColumn(UserRole attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getRoleName(); // Retorna "ROLE_ADMIN" ou "ROLE_USER"
    }

    @Override
    public UserRole convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        // Converte "ROLE_ADMIN" → UserRole.ADMIN
        // Converte "ROLE_USER" → UserRole.USER
        for (UserRole role : UserRole.values()) {
            if (role.getRoleName().equals(dbData)) {
                return role;
            }
        }

        throw new IllegalArgumentException("Unknown role: " + dbData);
    }
}