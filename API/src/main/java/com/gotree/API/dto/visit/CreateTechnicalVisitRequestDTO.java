package com.gotree.API.dto.visit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class CreateTechnicalVisitRequestDTO {

    @NotBlank(message = "O título da visita é obrigatório.")
    private String title;

    @NotNull(message = "O ID da empresa cliente é obrigatório.")
    private Long clientCompanyId;

    private Long unitId;
    private Long sectorId;
    private String location;

    @NotNull(message = "A data da visita é obrigatória.")
    private LocalDate visitDate;

    @NotNull(message = "A hora da visita é obrigatória.")
    private LocalTime startTime;

    private String technicalReferences;
    private String summary;

    @NotEmpty
    private List<VisitFindingDTO> findings;

    // Assinaturas
    private String technicianSignatureImageBase64;
    private String clientSignatureImageBase64;
    private String clientSignerName;
    private Double clientSignatureLatitude;
    private Double clientSignatureLongitude;

    // Agendamento da próxima visita
    private LocalDate nextVisitDate;
    private String nextVisitShift;
}