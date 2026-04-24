package com.gotree.API.dto.agenda;

import com.gotree.API.enums.AgendaEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Schema(description = "DTO para criação de eventos na agenda")
public class CreateEventDTO {
    @Schema(description = "Título do evento")
    private String title;

    @Schema(description = "Descrição do evento")
    private String description;

    @Schema(description = "Data do evento", example = "2024-01-15")
    @NotNull(message = "A data é obrigatória.")
    private LocalDate eventDate;

    @Schema(description = "Horário do evento", type = "string", format = "time", example = "14:30:00")
    private LocalTime eventHour;

    @Schema(description = "Tipo do evento (Valores automáticos via ENUM", example = "EVENTO")
    @NotNull(message = "O tipo de evento é obrigatório.")
    private AgendaEventType eventType;

    @Schema(description = "Turno do evento", example = "MANHA", allowableValues = {"MANHA", "TARDE"})
    private String shift;

    @Schema(description = "ID da Empresa associada ao evento")
    @NotNull(message = "A empresa é obrigatória.")
    private Long companyId;

    @Schema(description = "ID da Unidade associada ao evento")
    private Long unitId;

    @Schema(description = "ID do Setor associado ao evento")
    private Long sectorId;
}