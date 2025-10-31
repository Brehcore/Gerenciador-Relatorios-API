package com.gotree.API.dto.visit;

import lombok.Data;
import java.time.LocalDate;

@Data
public class VisitFindingDTO {
    private String photoBase64_1; // A imagem virá como Base64 do frontend
    private String photoBase64_2;
    private String description;
    private String consequences;
    private String legalGuidance;
    private String responsible;
    private String penalties;
    private String priority; // "BAIXA", "MEDIA", ou "ALTA"
    private LocalDate deadline;
    private boolean recurrence;
}