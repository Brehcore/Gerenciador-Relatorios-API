package com.gotree.API.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
@Schema(description = "DTO que associa o nome da empresa à quantidade de documentos")
public class CompanyCountDTO {

    @Schema(description = "Nome da empresa", example = "Empresa Exemplo")
    private String companyName;

    @Schema(description = "Quantidade de documentos associados")
    private long documentCount;
}