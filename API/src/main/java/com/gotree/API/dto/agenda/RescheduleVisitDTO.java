package com.gotree.API.dto.agenda;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Schema(description = "DTO para reagendamento de visita técnica")
public class RescheduleVisitDTO {

    @NotNull(message = "A nova data não pode ser nula.")
    @Schema(description = "Nova data da visita", example = "2024-04-10")
    private LocalDate newDate;

    @Schema(description = "Novo turno da visita", example = "TARDE")
    private String shift;

    @Schema(description = "Motivo do reagendamento", example = "Cliente pediu adiamento")
    private String reason;

    @Schema(description = "Horário da próxima visita", type = "string", format = "time", example = "14:30:00")
    private LocalTime eventHour;
}