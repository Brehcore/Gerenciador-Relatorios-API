package com.gotree.API.dto.report;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class AepRequestDTO {
    private Long companyId;
    private LocalDate evaluationDate; // Data da Avaliação
    private String evaluatedFunction; // Função Avaliada
    private List<String> selectedRiskIds; // Apenas os IDs dos riscos selecionados

    // --- Campos para a Fisioterapeuta
    private String physioName;
    private String physioCrefito;
}