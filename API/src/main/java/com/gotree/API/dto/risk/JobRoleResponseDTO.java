package com.gotree.API.dto.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "DTO de resposta para listagem de funções de trabalho")
public class JobRoleResponseDTO {

    @Schema(description = "ID único da função", example = "1")
    private Long id;

    @Schema(description = "Nome da função", example = "Auxiliar Administrativo")
    private String name;

    @Schema(description = "ID da empresa associada", example = "10")
    private Long companyId;

    @Schema(description = "Nome da empresa associada", example = "Minha Empresa LTDA")
    private String companyName;
}