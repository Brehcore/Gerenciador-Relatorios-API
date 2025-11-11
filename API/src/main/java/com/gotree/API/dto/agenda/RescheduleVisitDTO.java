package com.gotree.API.dto.agenda;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class RescheduleVisitDTO {

    @NotNull(message = "A nova data não pode ser nula.")
    private LocalDate newDate; // O "Novo agendamento"

    private String reason; // Opcional, para adicionar ao log/descrição
}