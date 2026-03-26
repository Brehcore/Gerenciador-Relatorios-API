package com.gotree.API.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "DTO para requisição de alteração de senha")
public class ChangePasswordRequestDTO {

    @NotBlank(message = "A nova senha é obrigatória.")
    @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres.")
    @Schema(description = "Nova senha do usuário", example = "novaSenha123")
    private String newPassword;

    @NotBlank
    @Schema(description = "Senha atual para confirmação", example = "senhaAntiga123")
    private String currentPassword;

}