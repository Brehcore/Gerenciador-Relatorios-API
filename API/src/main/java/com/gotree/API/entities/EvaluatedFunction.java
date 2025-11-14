package com.gotree.API.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

/**
 * Entidade que representa uma função avaliada no contexto de riscos ocupacionais.
 * Esta classe armazena informações sobre a avaliação de riscos de uma função específica,
 * incluindo os códigos de risco selecionados e sua relação com o relatório ocupacional.
 */
@Entity
@Table(name = "tb_risk_evaluated_function")
@Data
public class EvaluatedFunction {
    

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String functionName; // O nome da função no momento da avaliação

    @ElementCollection
    @CollectionTable(name = "tb_risk_selected_codes", joinColumns = @JoinColumn(name = "evaluated_function_id"))
    @Column(name = "risk_code")
    private List<Integer> selectedRiskCodes; // Armazena os códigos (1 a 103)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    private OccupationalRiskReport report;
}