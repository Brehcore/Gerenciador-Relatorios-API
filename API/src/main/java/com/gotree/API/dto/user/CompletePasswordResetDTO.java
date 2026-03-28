package com.gotree.API.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Payload para concluir o fluxo de redefinição de senha forçada pelo administrador.")
public class CompletePasswordResetDTO {

    @Schema(
            description = "A nova senha definitiva escolhida pelo usuário",
            example = "NovaSenha123@",
            minLength = 6,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "A nova senha é obrigatória.")
    @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres.")
    private String newPassword;
}