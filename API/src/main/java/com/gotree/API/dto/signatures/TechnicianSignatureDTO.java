package com.gotree.API.dto.signatures;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Data
@Schema(description = "DTO para coleta da assinatura do técnico")
public class TechnicianSignatureDTO {

    @Schema(description = "Imagem da assinatura do técnico em Base64")
    private String imageBase64;
}
