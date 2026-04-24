package com.gotree.API.dto.visit;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Schema(description = "DTO para requisição de criação de visita técnica")
public class CreateTechnicalVisitRequestDTO {

    @NotBlank(message = "O título da visita é obrigatório.")
    @Schema(description = "Título da visita técnica", example = "Visita de Rotina - Unidade Sul")
    private String title;

    @NotNull(message = "O ID da empresa cliente é obrigatório.")
    @Schema(description = "ID da empresa cliente atendida", example = "10")
    private Long clientCompanyId;

    @Schema(description = "ID da unidade atendida", example = "5")
    private Long unitId;

    @Schema(description = "ID do setor atendido", example = "3")
    private Long sectorId;

    @Schema(description = "Localização da visita", example = "Galpão de Produção")
    private String location;

    @NotNull(message = "A data da visita é obrigatória.")
    @Schema(description = "Data da realização da visita", example = "2024-03-26")
    private LocalDate visitDate;

    @NotNull(message = "A hora da visita é obrigatória.")
    @Schema(description = "Hora de início da visita", example = "09:00:00")
    private LocalTime startTime;

    @Schema(description = "Referências técnicas observadas")
    private String technicalReferences;

    @Schema(description = "Resumo da visita técnica")
    private String summary;

    @NotEmpty
    @Schema(description = "Lista de achados e recomendações da visita")
    private List<VisitFindingDTO> findings;

    @Schema(description = "Assinatura do técnico em Base64")
    private String technicianSignatureImageBase64;

    @Schema(description = "Assinatura do cliente em Base64")
    private String clientSignatureImageBase64;

    @Schema(description = "Nome de quem assinou pelo cliente", example = "Ricardo Oliveira")
    private String clientSignerName;

    @Schema(description = "Latitude da assinatura do cliente")
    private Double clientSignatureLatitude;

    @Schema(description = "Longitude da assinatura do cliente")
    private Double clientSignatureLongitude;

    @Schema(description = "Data sugerida para a próxima visita", example = "2024-06-26")
    private LocalDate nextVisitDate;

    @Schema(description = "Horário previsto para o evento no calendário do técnico (Opcional)", type = "string", format = "time", example = "09:00:00")
    private LocalTime eventHour;

    @Schema(description = "Turno sugerido para a próxima visita", example = "MANHA")
    private String nextVisitShift;
}