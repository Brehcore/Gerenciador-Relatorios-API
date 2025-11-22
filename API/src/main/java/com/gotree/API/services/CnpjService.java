package com.gotree.API.services;

import com.gotree.API.dto.external.CnpjResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class CnpjService {

    private final RestTemplate restTemplate;

    public CnpjService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public CnpjResponseDTO consultCnpj(String cnpj) {
        // 1. Remove tudo que não for número para evitar erros na URL
        String cleanCnpj = cnpj.replaceAll("[^0-9]", "");

        // 2. URL da BrasilAPI (Rápida e sem necessidade de token)
        String url = "https://brasilapi.com.br/api/cnpj/v1/" + cleanCnpj;

        try {
            // 3. O RestTemplate faz a mágica de converter o JSON para o seu DTO
            return restTemplate.getForObject(url, CnpjResponseDTO.class);

        } catch (HttpClientErrorException e) {
            // Se der erro (ex: CNPJ não existe), retorna null ou lança erro tratado
            throw new RuntimeException("CNPJ não encontrado ou inválido.");
        }
    }
}