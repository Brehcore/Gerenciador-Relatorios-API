package com.gotree.API.modules.shared.exceptions;

import lombok.Getter;

@Getter
public class ValidationError {
    private String field;
    private String message;

    public ValidationError(String field, String message) {
        this.field = field;
        this.message = message;
    }

}