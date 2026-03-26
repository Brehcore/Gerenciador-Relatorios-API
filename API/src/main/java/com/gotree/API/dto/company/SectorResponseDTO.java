package com.gotree.API.dto.company;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "DTO de resposta contendo os detalhes do setor")
public class SectorResponseDTO {
    @Schema(description = "ID único do setor", example = "1")
    private Long id;

    @Schema(description = "Nome do setor", example = "Produção")
    private String name;
}