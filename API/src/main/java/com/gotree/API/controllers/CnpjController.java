package com.gotree.API.controllers;

import com.gotree.API.dto.external.CnpjResponseDTO;
import com.gotree.API.services.CnpjService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST responsável por gerenciar consultas de informações de CNPJ.
 * Este controlador fornece endpoints para:
 * - Consulta de informações de empresas através do CNPJ
 * Os dados são obtidos por serviços externos de consulta de CNPJ.
 */
@Tag(name = "Consultas de CNPJ", description = "Gerenciamento de consultas de informações do CNPJ")
@RestController
@RequestMapping("/external/cnpj")
public class CnpjController {

    private final CnpjService cnpjService;

    public CnpjController(CnpjService cnpjService) {
        this.cnpjService = cnpjService;
    }

    /**
     * Consulta informações de uma empresa através do seu CNPJ.
     * Este endpoint recebe um número de CNPJ e retorna os dados cadastrais
     * da empresa correspondente, obtidos de serviços externos de consulta.
     *
     * @param cnpj o número do CNPJ da empresa a ser consultada
     * @return ResponseEntity contendo os dados cadastrais da empresa
     */
    @Operation(summary = "Consulta o CNPJ", description = "Consulta informações de uma empresa através do seu CNPJ")
    @GetMapping("/{cnpj}")
    public ResponseEntity<CnpjResponseDTO> getCompanyInfo(@PathVariable String cnpj) {
        CnpjResponseDTO response = cnpjService.consultCnpj(cnpj);
        return ResponseEntity.ok(response);
    }
}