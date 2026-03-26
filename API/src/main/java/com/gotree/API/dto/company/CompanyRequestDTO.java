package com.gotree.API.dto.company;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "DTO para requisição de criação/atualização de empresa")
public class CompanyRequestDTO {

    @NotBlank(message = "O nome da empresa é obrigatório.")
    @Schema(description = "Nome da empresa", example = "Minha Empresa LTDA")
    private String name;

    @NotBlank(message = "O CNPJ da empresa é obrigatório.")
    @Schema(description = "CNPJ da empresa", example = "12.345.678/0001-99")
    private String cnpj;

    @Valid
    @Schema(description = "Lista de unidades da empresa")
    private List<UnitDTO> units;

    @Schema(description = "Lista de nomes dos setores da empresa")
    private List<String> sectors;
}