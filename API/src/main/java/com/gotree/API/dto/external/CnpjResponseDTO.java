package com.gotree.API.dto.external; // Sugestão de pacote

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CnpjResponseDTO {

    @JsonProperty("razao_social")
    private String name; // Mapeia a razão social para "name"

    @JsonProperty("cnpj")
    private String cnpj;

}