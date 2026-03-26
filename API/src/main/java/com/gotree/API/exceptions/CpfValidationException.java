package com.gotree.API.exceptions;

import java.io.Serial;

public class CpfValidationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public CpfValidationException(String message) {
        super(message);
    }
}
