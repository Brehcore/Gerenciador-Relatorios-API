package com.gotree.API.modules.iam.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "DTO para requisição de login")
@Getter
@Setter
public class LoginRequestDTO {

    @NotBlank
    @Email
    @Schema(description = "E-mail do usuário", example = "usuario@exemplo.com")
    private String email;

    @NotBlank
    @Schema(description = "Senha do usuário", example = "senha123")
    private String password;

}
