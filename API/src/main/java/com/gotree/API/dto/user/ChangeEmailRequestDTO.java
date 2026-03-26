package com.gotree.API.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "DTO para requisição de alteração de e-mail")
public class ChangeEmailRequestDTO {

    @Email
    @NotBlank
    @Schema(description = "Novo e-mail do usuário", example = "novo@email.com")
    private String newEmail;

    @NotBlank
    @Schema(description = "Senha atual para confirmação", example = "senha123")
    private String currentPassword;
}
