package com.gotree.API.dto.aep;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "DTO responsável pela requisição de criação de um relatório AEP (Análise Ergonômica Preliminar)")
public class AepRequestDTO {
    @Schema(description = "ID da empresa a ser avaliada", example = "10")
    private Long companyId;

    @NotNull(message = "A data da Avaliação é obrigatória.")
    @Schema(description = "Data em que a avaliação foi realizada", example = "2023-10-27")
    private LocalDate evaluationDate;

    @Schema(description = "Função ou setor que será avaliado", example = "Operador de Máquinas")
    private String evaluatedFunction;

    @Schema(description = "Lista de IDs dos riscos ergonômicos selecionados na avaliação", example = "[\"1\", \"3\", \"5\"]")
    private List<String> selectedRiskIds;

    @Schema(description = "ID do fisioterapeuta responsável pela avaliação (opcional)", example = "5")
    private Long physiotherapistId;
}