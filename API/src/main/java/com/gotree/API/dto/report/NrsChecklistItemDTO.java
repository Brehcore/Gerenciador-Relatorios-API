package com.gotree.API.dto.report;

import lombok.Data;

@Data
public class NrsChecklistItemDTO {
    private String description;

    // O front-end enviará "CONFORME", "NAO_CONFORME" ou "NAO_APLICA"
    private String status;

    // Opcional, apenas se status == "NAO_APLICA"
    private String justification;
}