package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.risk.SaveRiskReportRequestDTO;
import com.gotree.API.entities.OccupationalRiskReport;
import com.gotree.API.entities.User;
import com.gotree.API.services.RiskChecklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller responsável por gerenciar as operações relacionadas aos checklists de risco ocupacional.
 * Fornece endpoints para criação, atualização, consulta e assinatura digital de relatórios de risco.
 */
@Tag(name = "Checklist de Risco Ocupacional", description = "Gerenciamento de checklist de risco ocupacional")
@RestController
@RequestMapping("/risk-checklist")
public class RiskChecklistController {

    private final RiskChecklistService service;
    
    public RiskChecklistController(RiskChecklistService service) {
        this.service = service;
    }

    /**
     * Busca os dados detalhados de um relatório de risco para edição.
     *
     * @param id ID do relatório a ser consultado
     * @param authentication Objeto de autenticação contendo as informações do usuário atual
     * @return ResponseEntity contendo o DTO com os dados do relatório para edição
     */
    @Operation(summary = "Busca dados de um relatório", description = "Retorna os dados detalhados de um relatório de risco para edição.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_REPORTS') or hasRole('ADMIN')")
    public ResponseEntity<SaveRiskReportRequestDTO> getReportDetailsForEdit(@PathVariable Long id, Authentication authentication) {
        var user = ((CustomUserDetails) authentication.getPrincipal()).user();

        SaveRiskReportRequestDTO reportDto = service.findReportForEdit(id, user);

        return ResponseEntity.ok(reportDto);
    }

    /**
     * Cria um novo relatório de risco ocupacional e gera o PDF correspondente.
     *
     * @param dto DTO contendo os dados necessários para criar o relatório
     * @param authentication Objeto de autenticação contendo as informações do usuário atual
     * @return ResponseEntity contendo mensagem de sucesso e ID do relatório criado
     */
    @Operation(summary = "Cria novo relatório", description = "Criação de um novo relatório de risco ocupacional e gera o PDF correspondente.")
    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_REPORTS') or hasRole('ADMIN')")
    public ResponseEntity<?> create(@RequestBody SaveRiskReportRequestDTO dto, Authentication authentication) {
        var user = ((CustomUserDetails) authentication.getPrincipal()).user();
        OccupationalRiskReport report = service.createAndGeneratePdf(dto, user);

        return ResponseEntity.ok(Map.of("message", "Criado com sucesso", "id", report.getId()));
    }

    /**
     * Atualiza um relatório de risco ocupacional existente e regenera o PDF.
     *
     * @param id ID do relatório a ser atualizado
     * @param dto DTO contendo os novos dados do relatório
     * @param authentication Objeto de autenticação contendo as informações do usuário atual
     * @return ResponseEntity contendo mensagem de sucesso e ID do relatório atualizado
     */
    @Operation(summary = "Atualiza um relatório", description = "Atualiza um relatório de risco ocupacional existente e regenera o PDF.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDIT_REPORTS') or hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody SaveRiskReportRequestDTO dto, Authentication authentication) {
        var user = ((CustomUserDetails) authentication.getPrincipal()).user();
        OccupationalRiskReport report = service.updateReport(id, dto, user);

        return ResponseEntity.ok(Map.of("message", "Atualizado com sucesso", "id", report.getId()));
        
    }

    /**
     * Assina digitalmente um relatório de risco utilizando certificado digital padrão ICP-Brasil.
     *
     * @param id ID do relatório a ser assinado
     * @param authentication Objeto de autenticação contendo as informações do usuário atual
     * @return ResponseEntity contendo mensagem de sucesso ou erro detalhado em caso de falha
     * @throws IllegalStateException se o relatório não estiver em estado válido para assinatura
     * @throws IllegalArgumentException se os parâmetros fornecidos forem inválidos
     */
    @Operation(summary = "Assinatura digital em um relatório", description = "Assina digitalmente um relatório de risco utilizando certificado digital padrão ICP-Brasil")
    @PostMapping("/{id}/sign")
    @PreAuthorize("hasAuthority('CREATE_REPORTS') or hasRole('ADMIN')")
    public ResponseEntity<?> signReport(@PathVariable Long id, Authentication authentication) {
        try {
            User user = ((CustomUserDetails) authentication.getPrincipal()).user();
            service.signExistingReport(id, user);
            return ResponseEntity.ok(Map.of("message", "Checklist assinado com sucesso via Certificado Digital."));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erro ao processar assinatura: " + e.getMessage()));
        }
    }
}