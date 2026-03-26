package com.gotree.API.dto.company;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "DTO que representa uma unidade de uma empresa")
public class UnitDTO {

    @Schema(description = "ID único da unidade", example = "1")
    private Long id;

    @NotBlank(message = "O nome da unidade é obrigatório.")
    @Schema(description = "Nome da unidade", example = "Unidade Matriz")
    private String name;

    @Schema(description = "CNPJ da unidade (opcional)", example = "12.345.678/0002-00")
    private String cnpj;
}