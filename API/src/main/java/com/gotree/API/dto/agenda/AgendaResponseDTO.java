package com.gotree.API.dto.agenda;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AgendaResponseDTO {
    private String title;
    private LocalDate date;
    private String type; // "VISITA_TECNICA", "EVENTO", etc.
    private String description;
    private String shift; // "MANHA" ou "TARDE"

    // --- NOVOS CAMPOS PARA O RELATÓRIO E FRONTEND ---
    private String status; // O enum cru: "REAGENDADO", "CONFIRMADO"
    private String statusDescricao; // O texto formatado: "Reagendado p/ 15/02"
    // -----------------------------------------------

    private Long referenceId; // ID do AgendaEvent ou TechnicalVisit
    private String unitName;
    private String sectorName;
    private String clientName;

    private LocalDate originalVisitDate;
    private Long sourceVisitId;

    private String responsibleName;
}