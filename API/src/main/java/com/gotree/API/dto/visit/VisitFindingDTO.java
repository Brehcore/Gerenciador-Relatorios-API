package com.gotree.API.dto.visit;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;

@Data
@Schema(description = "DTO que representa um achado ou irregularidade encontrada na visita")
public class VisitFindingDTO {

    @Schema(description = "Primeira foto do achado em Base64")
    private String photoBase64_1;

    @Schema(description = "Segunda foto do achado em Base64")
    private String photoBase64_2;

    @Schema(description = "Descrição detalhada da irregularidade")
    private String description;

    @Schema(description = "Consequências da irregularidade")
    private String consequences;

    @Schema(description = "Orientação legal ou técnica")
    private String legalGuidance;

    @Schema(description = "Responsável pela regularização")
    private String responsible;

    @Schema(description = "Possíveis penalidades")
    private String penalties;

    @Schema(description = "Prioridade da regularização", example = "MEDIA")
    private String priority;

    @Schema(description = "Prazo para regularização", example = "2024-04-26")
    private LocalDate deadline;

    @Schema(description = "Indica se há reincidência")
    private boolean recurrence;
}