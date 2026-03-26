package com.gotree.API.dto.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "DTO para requisição de função avaliada e seus riscos")
public class EvaluatedFunctionRequestDTO {

    @Schema(description = "Nome da função avaliada", example = "Operador de Empilhadeira")
    private String functionName;

    @Schema(description = "Lista de códigos dos riscos selecionados")
    private List<Integer> selectedRiskCodes;
}