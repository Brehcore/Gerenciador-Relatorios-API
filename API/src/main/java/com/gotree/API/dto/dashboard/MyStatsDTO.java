package com.gotree.API.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "DTO contendo estatísticas pessoais do usuário")
public class MyStatsDTO {

    @Schema(description = "Total de visitas realizadas")
    private long totalVisits;

    @Schema(description = "Total de relatórios AEP gerados")
    private long totalAeps;

    @Schema(description = "Total de relatórios de risco gerados")
    private long totalRisks;

    @Schema(description = "Total de horas em visitas técnicas")
    private long totalVisitTimeHours;

    @Schema(description = "Total de minutos restantes em visitas técnicas")
    private long totalVisitTimeMinutes;

    @Schema(description = "Lista das empresas com mais documentos")
    private List<CompanyCountDTO> topCompanies;
}