package com.gotree.API.enums;

import lombok.Getter;

@Getter
public enum UserRole {

    ADMIN("ADMIN"), USER("USER");

    private final String roleName;

    UserRole(String roleName) {
        this.roleName = roleName;
    }

}

