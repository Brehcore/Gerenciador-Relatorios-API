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
        String cleanCnpj = cnpj.replaceAll("[^0-9]", "");

        String url = "https://brasilapi.com.br/api/cnpj/v1/{cnpj}";

        try {
            return restTemplate.getForObject(url, CnpjResponseDTO.class, cleanCnpj);

        } catch (HttpClientErrorException e) {
            throw new RuntimeException("CNPJ não encontrado ou inválido.");
        }
    }
}