package com.gotree.API.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "DTO contendo estatísticas gerais para o administrador")
public class AdminStatsDTO {

    @Schema(description = "Total de usuários cadastrados")
    private long totalUsers;

    @Schema(description = "Total de empresas cadastradas")
    private long totalCompanies;

    @Schema(description = "Total de documentos gerados")
    private long totalDocuments;

    @Schema(description = "Total de horas de visitas técnicas")
    private long totalVisitTimeHours;

    @Schema(description = "Total de minutos restantes de visitas técnicas")
    private long totalVisitTimeMinutes;
}