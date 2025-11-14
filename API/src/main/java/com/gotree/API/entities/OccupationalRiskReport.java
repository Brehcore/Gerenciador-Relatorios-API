package com.gotree.API.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade que representa um Relatório de Riscos Ocupacionais.
 * Esta classe armazena informações sobre avaliações de riscos ocupacionais realizadas em diferentes setores
 * e funções de uma empresa, incluindo detalhes da inspeção, assinaturas e funções avaliadas.
 *
 * @see Company
 * @see Unit
 * @see Sector
 * @see User
 * @see EvaluatedFunction
 */
@Entity
@Table(name = "tb_occupational_risk_report")
@Data
public class OccupationalRiskReport {
    
    

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    /**
     * Identificador único do relatório.
     */
    private Long id;

    /**
     * Título padrão do relatório.
     */
    private String title = "Checklist - Riscos Ocupacionais";

    /**
     * Data em que a inspeção foi realizada.
     */
    private LocalDate inspectionDate;

    /**
     * Caminho do arquivo PDF gerado para este relatório.
     */
    private String pdfPath;

    /**
     * Empresa onde a avaliação foi realizada.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    private Company company;
    @ManyToOne(fetch = FetchType.LAZY)
    private Unit unit;
    @ManyToOne(fetch = FetchType.LAZY)
    private Sector sector;

    /**
     * Técnico responsável pela avaliação.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    private User technician;

    /**
     * Assinatura do técnico em formato Base64.
     */
    @Column(columnDefinition = "TEXT")
    private String technicianSignatureImageBase64;
    private LocalDateTime technicianSignedAt;

    // Lista de funções avaliadas neste relatório
    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EvaluatedFunction> evaluatedFunctions = new ArrayList<>();
}