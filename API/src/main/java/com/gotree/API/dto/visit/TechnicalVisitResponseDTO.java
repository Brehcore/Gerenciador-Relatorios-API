package com.gotree.API.dto.visit;

import lombok.Data;
import java.time.LocalDate;

@Data
public class TechnicalVisitResponseDTO {

    private Long id;
    private String title;
    private String clientCompanyName;
    private LocalDate visitDate;
    private String documentType = "Relatório de Visita"; // Campo fixo para identificar o tipo no frontend

}