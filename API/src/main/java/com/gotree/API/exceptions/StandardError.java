package com.gotree.API.exceptions;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.util.HtmlUtils;

@Getter
@Setter
public class StandardError {

    private Instant timestamp;
    private Integer status;
    private String error;
    private String message;
    private String path;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<ValidationError> validationErrors = new ArrayList<>();

    public StandardError(Instant timestamp, Integer status, String error, String message, String path) {
        super();
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message != null ? HtmlUtils.htmlEscape(message) : null;
        this.path = path != null ? HtmlUtils.htmlEscape(path) : null;

    }

    public void addValidationError(String field, String message) {
        this.validationErrors.add(new ValidationError(field, message != null ? HtmlUtils.htmlEscape(message) : null));
    }

}