package com.gotree.API.enums;

import lombok.Getter;

@Getter
public enum UserRole {

    ADMIN("ROLE_ADMIN"), USER("ROLE_USER");

    private final String roleName;

    UserRole(String roleName) {
        this.roleName = roleName;
    }

}

