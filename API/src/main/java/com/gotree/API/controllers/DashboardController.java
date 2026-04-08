package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.dashboard.AdminStatsDTO;
import com.gotree.API.dto.dashboard.MyStatsDTO;
import com.gotree.API.dto.dashboard.UserDocumentStatsDTO;
import com.gotree.API.entities.User;
import com.gotree.API.services.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST responsável pelos endpoints do dashboard da aplicação.
 * Fornece estatísticas e KPIs tanto para usuários técnicos autenticados
 * quanto para administradores do sistema.
 * Todos os endpoints requerem autenticação e alguns requerem permissões específicas.
 */
@Tag(name = "Dashboard", description = "Fornece estatísticas e KPIs tanto para usuários técnicos autenticados quanto para administradores.")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Retorna os KPIs (Key Performance Indicators) do usuário logado.
     * Este endpoint está disponível para qualquer usuário autenticado (Técnico).
     * As estatísticas retornadas são específicas do usuário atual e incluem
     * métricas relacionadas às suas atividades e documentos.
     *
     * @param authentication objeto de autenticação contendo os dados do usuário logado
     * @return ResponseEntity contendo MyStatsDTO com as estatísticas do usuário
     */
    @Operation(summary = "Retorna os KPIs", description = "Retorna as KPIs para qualquer usuário autenticado (Técnico).")
    @GetMapping("/my-stats")
    @PreAuthorize("hasAuthority('VIEW_DASHBOARDS') or hasRole('ADMIN')")
    public ResponseEntity<MyStatsDTO> getMyStats(Authentication authentication) {
        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).user();
        MyStatsDTO stats = dashboardService.getMyStats(currentUser);
        return ResponseEntity.ok(stats);
    }

    /**
     * Retorna os KPIs globais do sistema.
     * Este endpoint está disponível apenas para usuários com perfil de administrador.
     * As estatísticas incluem dados agregados de todos os usuários e documentos do sistema,
     * fornecendo uma visão geral das operações e métricas globais.
     *
     * @return ResponseEntity contendo AdminStatsDTO com as estatísticas administrativas
     */
    @Operation(summary = "KPIs globais", description = "Retorna as KPIs globais para administradores do sistema.")
    @GetMapping("/admin-stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminStatsDTO> getAdminStats() {
        AdminStatsDTO stats = dashboardService.getAdminStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Retorna estatísticas detalhadas de documentos agrupadas por usuário.
     * Este endpoint está disponível apenas para usuários com perfil de administrador.
     * Permite filtrar as estatísticas por usuário específico, tipo de documento e empresa.
     * Os filtros são opcionais e podem ser combinados para refinar os resultados.
     *
     * @param userId identificador do usuário para filtrar as estatísticas (opcional)
     * @param type tipo de documento para filtrar ("VISIT" ou "AEP") (opcional)
     * @param companyId identificador da empresa para filtrar as estatísticas (opcional)
     * @return ResponseEntity contendo lista de UserDocumentStatsDTO com as estatísticas filtradas
     */
    @Operation(summary = "Filtrar Estatísticas", description = "Retorna estatísticas detalhadas de documentos agrupados por usuário.")
    @GetMapping("/admin-stats/documents")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDocumentStatsDTO>> getAdminDocumentStats(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String type, // "VISIT" ou "AEP"
            @RequestParam(required = false) Long companyId
    ) {
        List<UserDocumentStatsDTO> stats = dashboardService.getAdminDocumentStats(userId, type, companyId);
        return ResponseEntity.ok(stats);
    }
}