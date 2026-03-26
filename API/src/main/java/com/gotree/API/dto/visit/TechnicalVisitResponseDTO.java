package com.gotree.API.dto.visit;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;

@Data
@Schema(description = "DTO de resposta simplificada de visita técnica")
public class TechnicalVisitResponseDTO {

    @Schema(description = "ID único da visita", example = "1")
    private Long id;

    @Schema(description = "Título da visita técnica", example = "Visita de Rotina")
    private String title;

    @Schema(description = "Nome da empresa cliente atendida", example = "Empresa Exemplo LTDA")
    private String clientCompanyName;

    @Schema(description = "Data da realização da visita", example = "2024-03-26")
    private LocalDate visitDate;

    @Schema(description = "Tipo do documento", example = "Relatório de Visita")
    private String documentType = "Relatório de Visita";

    @Schema(description = "Indica se o documento possui assinatura ICP-Brasil")
    private boolean icpSigned;
}