package com.gotree.API.dto.agenda;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AgendaResponseDTO {
    private String title;
    private LocalDate date;
    private String type; // "EVENTO" ou "VISITA"
    private String description; // Nulo para visitas
    private Long referenceId; // O ID original (do Evento ou da Visita)

    private String unitName; // Null se for evento
    private String sectorName; // Null se for evento
}