package com.gotree.API.dto.agenda;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "DTO para cancelamento de eventos na agenda")
public class CancelEventDTO {

    @Schema(description = "Motivo do cancelamento", example = "Cliente solicitou cancelamento via telefone.")
    @NotBlank(message = "O motivo do cancelamento é obrigatório.")
    private String reason;
}