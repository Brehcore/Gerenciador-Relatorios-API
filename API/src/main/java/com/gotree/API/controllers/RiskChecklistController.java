package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.risk.SaveRiskReportRequestDTO;
import com.gotree.API.entities.OccupationalRiskReport;
import com.gotree.API.services.RiskChecklistService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller responsável por gerenciar as operações relacionadas aos checklists de risco ocupacional.
 * Fornece endpoints para criação e gerenciamento de relatórios de risco.
 */
@RestController
@RequestMapping("/risk-checklist")
public class RiskChecklistController {

    private final RiskChecklistService service;

    /**
     * Construtor que inicializa o controller com o serviço necessário.
     *
     * @param service Serviço que contém a lógica de negócios para checklist de riscos
     */
    public RiskChecklistController(RiskChecklistService service) {
        this.service = service;
    }

    /**
     * Cria um novo relatório de risco ocupacional e gera o PDF correspondente.
     *
     * @param dto            DTO contendo os dados necessários para criar o relatório
     * @param authentication Objeto de autenticação contendo as informações do usuário atual
     * @return ResponseEntity contendo mensagem de sucesso e ID do relatório criado
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody SaveRiskReportRequestDTO dto, Authentication authentication) {
        var user = ((CustomUserDetails) authentication.getPrincipal()).user();
        OccupationalRiskReport report = service.createAndGeneratePdf(dto, user);

        return ResponseEntity.ok(Map.of("message", "Criado com sucesso", "id", report.getId()));
    }
}