package com.gotree.API.dto.agenda;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

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

    @Schema(description = "Tipo do evento", example = "EVENTO", allowableValues = {"EVENTO", "TREINAMENTO"})
    @NotBlank(message = "O tipo de evento é obrigatório.")
    private String eventType;

    @Schema(description = "Turno do evento", example = "MANHA", allowableValues = {"MANHA", "TARDE"})
    private String shift;

    @Schema(description = "Nome do cliente associado ao evento")
    private String clientName;

    @Schema(description = "Observação manual sobre o evento")
    private String manualObservation;
}