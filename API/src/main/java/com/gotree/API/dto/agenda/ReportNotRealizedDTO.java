package com.gotree.API.dto.agenda;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "DTO para registrar o motivo de uma visita não realizada")
public class ReportNotRealizedDTO {

    @NotBlank(message = "O motivo da não realização é obrigatório.")
    @Schema(description = "Motivo pelo qual a visita não pôde ser executada", example = "O local estava fechado e o responsável não atendeu o telefone.")
    private String reason;
}