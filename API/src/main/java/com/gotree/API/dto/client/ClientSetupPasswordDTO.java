package com.gotree.API.dto.client;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "DTO para configuração de senha do cliente")
public class ClientSetupPasswordDTO {

    @NotBlank @Email
    @Schema(description = "E-mail do cliente", example = "cliente@exemplo.com")
    private String email;

    @NotBlank
    @Schema(description = "Código de acesso recebido por e-mail", example = "123456")
    private String accessCode;

    @NotBlank
    @Schema(description = "Nova senha do cliente", example = "novaSenha123")
    private String newPassword;
}
