package com.gotree.API.modules.iam.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@Schema(description = "DTO para upload de certificado digital")
public class CertificateUploadDTO {

    @NotBlank(message = "A senha do certificado é obrigatória")
    @Schema(description = "Senha do certificado digital", example = "senha123")
    private String password;

    @Schema(description = "Arquivo do certificado (.pfx ou .p12)")
    private MultipartFile file;
}