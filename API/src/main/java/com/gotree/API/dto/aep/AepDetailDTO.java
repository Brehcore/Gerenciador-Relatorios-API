package com.gotree.API.dto.aep;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "DTO que contém os detalhes completos de um relatório AEP (Análise Ergonômica Preliminar)")
public class AepDetailDTO {
    @Schema(description = "ID único do relatório AEP", example = "1")
    private Long id;

    @Schema(description = "ID da empresa avaliada", example = "10")
    private Long companyId;

    @Schema(description = "Nome da empresa avaliada", example = "Empresa de Exemplo LTDA")
    private String companyName;

    @Schema(description = "CNPJ da empresa avaliada", example = "12.345.678/0001-99")
    private String companyCnpj;

    @Schema(description = "Data em que a avaliação foi realizada", example = "2023-10-27")
    private LocalDate evaluationDate;

    @Schema(description = "Função ou setor que foi avaliado", example = "Operador de Máquinas")
    private String evaluatedFunction;

    @Schema(description = "ID do fisioterapeuta responsável (opcional)", example = "5")
    private Long physiotherapistId;

    @Schema(description = "Lista de riscos ergonômicos selecionados na avaliação", example = "[\"Postura inadequada\", \"Repetitividade\"]")
    private List<String> selectedRisks;

    @Schema(description = "ID do usuário que realizou a avaliação", example = "2")
    private Long evaluatorId;

    @Schema(description = "Nome do usuário que realizou a avaliação", example = "João da Silva")
    private String evaluatorName;
}