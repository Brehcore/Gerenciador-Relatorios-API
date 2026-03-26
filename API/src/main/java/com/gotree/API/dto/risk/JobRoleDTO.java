package com.gotree.API.dto.risk;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "DTO que representa uma função de trabalho")
public class JobRoleDTO {

    @Schema(description = "ID único da função", example = "1")
    private Long id;

    @Schema(description = "Nome da função", example = "Auxiliar Administrativo")
    private String name;

    @Schema(description = "ID da empresa associada", example = "10")
    private Long companyId;
}