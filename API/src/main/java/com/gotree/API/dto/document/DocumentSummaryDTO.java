package com.gotree.API.dto.document; // Crie este novo pacote

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "DTO que representa o resumo de um documento para listagem")
public class DocumentSummaryDTO {

    @Schema(description = "ID único do documento", example = "1")
    private Long id;

    @Schema(description = "Tipo do documento", example = "Checklist de Inspeção")
    private String documentType;

    @Schema(description = "Título do documento", example = "Inspeção de Segurança - Unidade A")
    private String title;

    @Schema(description = "Nome do cliente associado", example = "Empresa Exemplo")
    private String clientName;

    @Schema(description = "Data de criação do documento", example = "2024-03-26")
    private LocalDate creationDate;

    @Schema(description = "Indica se o documento foi assinado digitalmente")
    private boolean signed;

    @Schema(description = "Indica se o documento possui assinatura ICP-Brasil")
    private boolean icpSigned;

    @Schema(description = "Indica se o arquivo PDF foi gerado")
    private boolean pdfGenerated;

    @Schema(description = "Data e hora da assinatura ICP-Brasil")
    private LocalDateTime icpSignedAt;

    @Schema(description = "Indica se o e-mail foi enviado para o cliente")
    private boolean emailSent;

    @Schema(description = "E-mail do cliente para o qual o documento foi enviado")
    private String clientEmail;

    @Schema(description = "Data e hora do envio do e-mail")
    private LocalDateTime sentAt;

    @Schema(description = "Nome do técnico responsável")
    private String technicianName;
}