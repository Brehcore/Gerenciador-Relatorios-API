package com.gotree.API.dto.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClientSetupPasswordDTO {

    @NotBlank @Email
    private String email;

    @NotBlank
    private String accessCode;

    @NotBlank
    private String newPassword;
}
