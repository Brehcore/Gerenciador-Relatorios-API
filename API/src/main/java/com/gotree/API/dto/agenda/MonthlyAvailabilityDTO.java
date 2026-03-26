package com.gotree.API.dto.agenda;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "DTO que representa a disponibilidade mensal")
public class MonthlyAvailabilityDTO {

        @Schema(description = "Data da disponibilidade", example = "2024-03-26")
        private LocalDate date;

        @Schema(description = "Indica se o turno da manhã está ocupado")
        private boolean morningBusy;

        @Schema(description = "Indica se o turno da tarde está ocupado")
        private boolean afternoonBusy;

        @Schema(description = "Indica se o dia inteiro está ocupado")
        private boolean fullDayBusy;
}
