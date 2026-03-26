package com.gotree.API.dto.company;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "DTO de resposta contendo os detalhes da empresa")
public class CompanyResponseDTO {

    @Schema(description = "ID único da empresa", example = "1")
    private Long id;

    @Schema(description = "Nome da empresa", example = "Minha Empresa LTDA")
    private String name;

    @Schema(description = "CNPJ da empresa", example = "12.345.678/0001-99")
    private String cnpj;

    @Schema(description = "Lista de unidades da empresa")
    private List<UnitDTO> units;

    @Schema(description = "Lista de setores da empresa")
    private List<SectorResponseDTO> sectors;
}
