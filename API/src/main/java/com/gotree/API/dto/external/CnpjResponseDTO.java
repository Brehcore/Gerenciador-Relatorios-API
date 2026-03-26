package com.gotree.API.dto.external; // Sugestão de pacote

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "DTO de resposta da consulta externa de CNPJ")
public class CnpjResponseDTO {

    @JsonProperty("razao_social")
    @Schema(description = "Razão social da empresa", example = "Empresa Exemplo LTDA")
    private String name;

    @JsonProperty("cnpj")
    @Schema(description = "CNPJ da empresa", example = "12345678000199")
    private String cnpj;

}