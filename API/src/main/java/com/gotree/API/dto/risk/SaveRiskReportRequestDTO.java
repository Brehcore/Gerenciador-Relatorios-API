package com.gotree.API.dto.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "DTO para salvamento de relatório de risco")
public class SaveRiskReportRequestDTO {

    @Schema(description = "Título do relatório", example = "Relatório Trimestral - Unidade A")
    private String title;

    @Schema(description = "ID da empresa associada", example = "10")
    private Long companyId;

    @Schema(description = "ID da unidade associada", example = "5")
    private Long unitId;

    @Schema(description = "ID do setor associado", example = "3")
    private Long sectorId;

    @Schema(description = "Data em que a inspeção foi realizada", example = "2024-03-26")
    private LocalDate inspectionDate;

    @Schema(description = "Assinatura do técnico em Base64")
    private String technicianSignatureImageBase64;

    @Schema(description = "Lista de funções e riscos avaliados")
    private List<EvaluatedFunctionRequestDTO> functions;

    @Schema(description = "Indica se o relatório será assinado via ICP-Brasil")
    private boolean icpSigned;
}