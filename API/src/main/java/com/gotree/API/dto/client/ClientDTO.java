package com.gotree.API.dto.client;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "DTO que representa as informações de um cliente")
public class ClientDTO {

    @Schema(description = "ID único do cliente", example = "1")
    private Long id;

    @Schema(description = "Nome do cliente", example = "José Silva")
    private String name;

    @Schema(description = "E-mail do cliente", example = "jose@cliente.com")
    private String email;

    @Schema(description = "Lista de IDs das empresas vinculadas")
    private List<Long> companyIds;

    @Schema(description = "Lista de nomes das empresas vinculadas")
    private List<String> companyNames;
}