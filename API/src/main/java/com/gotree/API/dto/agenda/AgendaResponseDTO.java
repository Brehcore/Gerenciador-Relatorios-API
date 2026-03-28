package com.gotree.API.dto.agenda;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;

@Data
@Schema(description = "DTO de resposta contendo informações da agenda")
public class AgendaResponseDTO {
    @Schema(description = "Título do evento da agenda")
    private String title;

    @Schema(description = "Data do evento")
    private LocalDate date;

    @Schema(description = "Tipo do evento", example = "VISITA_TECNICA")
    private String type;

    @Schema(description = "Descrição detalhada do evento")
    private String description;

    @Schema(description = "Turno do evento", example = "MANHA")
    private String shift;

    @Schema(description = "Status do evento", example = "CONFIRMADO")
    private String status;

    @Schema(description = "Descrição formatada do status", example = "Reagendado p/ 15/02")
    private String statusDescricao;

    @Schema(description = "ID de referência do evento ou visita técnica")
    private Long referenceId;

    @Schema(description = "Nome da unidade")
    private String unitName;

    @Schema(description = "Nome do setor")
    private String sectorName;

    @Schema(description = "Nome do cliente")
    private String clientName;

    @Schema(description = "Data original da visita antes do reagendamento")
    private LocalDate originalVisitDate;

    @Schema(description = "ID da visita de origem")
    private Long sourceVisitId;

    @Schema(description = "Nome do responsável")
    private String responsibleName;

    @Schema(description = "ID do responsável")
    private Long responsibleId;
}