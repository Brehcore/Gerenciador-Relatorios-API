package com.gotree.API.dto.agenda;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Schema(description = "DTO de resposta contendo informações da agenda")
public class AgendaResponseDTO {
    @Schema(description = "Título do evento da agenda")
    private String title;

    @Schema(description = "Data do evento")
    private LocalDate date;

    @Schema(description = "Horário do evento", type = "string", format = "time", example = "14:30:00")
    private LocalTime eventHour; // Alterado de startTime para eventHour

    @Schema(description = "Tipo do evento", example = "VISITA_TECNICA")
    private String type;

    @Schema(description = "Descrição detalhada do evento")
    private String description;

    @Schema(description = "Turno do evento", example = "MANHA")
    private String shift;

    @Schema(description = "Status do evento", example = "CONFIRMADO")
    private String status;

    @Schema(description = "Indica se a visita foi realizada (true), não realizada (false) ou pendente (null)")
    private Boolean isRealized;

    @Schema(description = "Motivo caso a visita não tenha sido realizada")
    private String nonCompletionReason;

    @Schema(description = "Descrição formatada do status", example = "Reagendado p/ 15/02")
    private String statusDescricao;

    @Schema(description = "ID de referência do evento ou visita técnica")
    private Long referenceId;

    @Schema(description = "ID da Empresa associada")
    private Long companyId;

    @Schema(description = "CNPJ da empresa")
    private String companyCnpj;

    @Schema(description = "Nome da empresa")
    private String companyName;

    @Schema(description = "ID da Unidade associada")
    private Long unitId;

    @Schema(description = "CNPJ da unidade")
    private String unitCnpj;

    @Schema(description = "Nome da Unidade associada")
    private String unitName;

    @Schema(description = "ID do Setor associado")
    private Long sectorId;

    @Schema(description = "Nome do Setor associado")
    private String sectorName;

    @Schema(description = "Data original da visita antes do reagendamento")
    private LocalDate originalVisitDate;

    @Schema(description = "ID da visita de origem")
    private Long sourceVisitId;

    @Schema(description = "Nome do responsável")
    private String responsibleName;

    @Schema(description = "ID do responsável")
    private Long responsibleId;
}