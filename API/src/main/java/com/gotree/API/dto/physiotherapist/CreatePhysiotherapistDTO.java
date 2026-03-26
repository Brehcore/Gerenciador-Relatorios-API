package com.gotree.API.dto.physiotherapist;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Dados necessários para cadastrar um novo fisioterapeuta no sistema.")
public class CreatePhysiotherapistDTO {

    @Schema(description = "Nome completo do fisioterapeuta", example = "Ana Silva")
    @NotBlank(message = "O nome é obrigatório")
    private String name;

    @Schema(description = "Número de registo no CREFITO", example = "123456-F")
    @NotBlank(message = "O CREFITO é obrigatório")
    private String crefito;

}