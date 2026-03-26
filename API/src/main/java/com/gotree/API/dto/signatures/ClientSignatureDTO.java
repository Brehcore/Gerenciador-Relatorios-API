package com.gotree.API.dto.signatures;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "DTO para coleta da assinatura do cliente")
public class ClientSignatureDTO {

    @Schema(description = "Nome de quem assinou", example = "Carlos Souza")
    private String signerName;

    @Schema(description = "Imagem da assinatura em Base64")
    private String imageBase64;

    @Schema(description = "Latitude da localização no momento da assinatura")
    private Double latitude;

    @Schema(description = "Longitude da localização no momento da assinatura")
    private Double longitude;
}