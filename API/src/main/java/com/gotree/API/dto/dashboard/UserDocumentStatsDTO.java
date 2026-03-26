package com.gotree.API.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "DTO contendo estatísticas de documentos de um usuário específico")
public class UserDocumentStatsDTO {

    @Schema(description = "ID do usuário", example = "1")
    private Long userId;

    @Schema(description = "Nome do usuário", example = "João Silva")
    private String userName;

    @Schema(description = "Total de visitas técnicas realizadas")
    private long totalVisits;

    @Schema(description = "Total de relatórios AEP gerados")
    private long totalAeps;

    @Schema(description = "Total de relatórios de risco gerados")
    private long totalRisks;

    @Schema(description = "Total geral de documentos gerados")
    private long totalDocuments;

    public UserDocumentStatsDTO(Long userId, String userName) {
        this.userId = userId;
        this.userName = userName;
        this.totalVisits = 0;
        this.totalAeps = 0;
        this.totalRisks = 0;
        this.totalDocuments = 0;
    }
}