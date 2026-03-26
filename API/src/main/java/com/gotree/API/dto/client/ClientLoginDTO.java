package com.gotree.API.dto.client;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "DTO para login do cliente")
public class ClientLoginDTO {

    @NotBlank @Email
    @Schema(description = "E-mail do cliente", example = "cliente@exemplo.com")
    private String email;

    @NotBlank
    @Schema(description = "Senha do cliente", example = "senha123")
    private String password;
}
