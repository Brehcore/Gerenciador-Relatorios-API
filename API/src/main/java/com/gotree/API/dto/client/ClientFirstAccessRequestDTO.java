package com.gotree.API.dto.client;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "DTO para solicitação de primeiro acesso do cliente")
public class ClientFirstAccessRequestDTO {

    @NotBlank @Email
    @Schema(description = "E-mail do cliente", example = "cliente@exemplo.com")
    private String email;
}
