package com.gotree.API.dto.risk;

import lombok.Data;
import java.util.List;

@Data
public class EvaluatedFunctionRequestDTO {
    private String functionName; // O usuário seleciona ou digita
    private List<Integer> selectedRiskCodes; // IDs dos riscos (1, 5, 22...)
}