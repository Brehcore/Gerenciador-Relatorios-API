package com.gotree.API.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangeEmailRequestDTO {

    @Email
    @NotBlank
    private String newEmail;

    @NotBlank
    private String currentPassword;
}
