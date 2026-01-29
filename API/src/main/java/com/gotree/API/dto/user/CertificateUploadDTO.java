package com.gotree.API.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class CertificateUploadDTO {

    @NotBlank(message = "A senha do certificado é obrigatória")
    private String password;

    private MultipartFile file;
}