package com.gotree.API.dto.document;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "DTO para download de arquivos")
public class FileDownloadDTO {

    @Schema(description = "Nome do arquivo", example = "documento.pdf")
    private String filename;

    @Schema(description = "Conteúdo do arquivo em bytes")
    private byte[] data;
}