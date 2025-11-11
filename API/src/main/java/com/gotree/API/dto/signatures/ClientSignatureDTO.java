package com.gotree.API.dto.signatures;

import lombok.Data;

@Data
public class ClientSignatureDTO {
    private String signerName; // Nome do cliente que assinou
    private String imageBase64; // A imagem da assinatura
    private Double latitude;
    private Double longitude;
}