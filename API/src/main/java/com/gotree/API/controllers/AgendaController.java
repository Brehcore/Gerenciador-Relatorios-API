package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.agenda.AgendaResponseDTO;
import com.gotree.API.dto.agenda.CreateEventDTO;
import com.gotree.API.dto.agenda.MonthlyAvailabilityDTO;
import com.gotree.API.dto.agenda.RescheduleVisitDTO;
import com.gotree.API.entities.AgendaEvent;
import com.gotree.API.entities.User;
import com.gotree.API.services.AgendaService;
import com.gotree.API.services.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador responsável pelo gerenciamento do calendário dos técnicos e agendamento de visitas.
 * Este controlador oferece recursos para verificar disponibilidade,
 * criar, atualizar e remover eventos, reagendar visitas e consultar relatórios.
 */
@Tag(name = "Agenda e Visitas", description = "Gerenciamento do calendário dos técnicos e agendamento de visitas.")
@RestController
@RequestMapping("/api/agenda")
public class AgendaController {
    
    private final AgendaService agendaService;
    private final ReportService reportService;

    public AgendaController(AgendaService agendaService, ReportService reportService) {
        this.agendaService = agendaService;
        this.reportService = reportService;
    }

    /**
     * Verifica a disponibilidade de agenda para uma data e turno específicos.
     *
     * @param auth  Dados de autenticação do usuário
     * @param date  Data para verificação
     * @param shift Turno (MANHA ou TARDE)
     * @return 200 OK se disponível, 409 Conflict se indisponível
     */
    @Operation(summary = "Verifica disponibilidade", description = "Verifica se uma determinada data e turno estão disponíveis na agenda do usuário autenticado.")
    @GetMapping("/check-availability")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> checkAvailability(
            Authentication auth,
            @RequestParam LocalDate date,
            @RequestParam String shift
    ) {
        User user = ((CustomUserDetails) auth.getPrincipal()).user();

        String warning = agendaService.checkAvailability(user, date, shift);

        if (warning != null) {
            // 409 Conflict: Informa o frontend que a agenda está cheia
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", warning));
        }

        return ResponseEntity.ok().build();
    }

    /**
     * Cria um novo evento na agenda do usuário.
     *
     * @param dto            Dados do evento a ser criado
     * @param authentication Dados do usuário autenticado
     * @return Dados do evento criado
     */
    @Operation(summary = "Cria um novo evento", description = "Cria um novo evento ou agendamento de visita na agenda do usuário autenticado.")
    @PostMapping("/eventos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AgendaResponseDTO> createEvent(
            @RequestBody @Valid CreateEventDTO dto,
            Authentication authentication
    ) {
        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).user();
        AgendaEvent newEvent = agendaService.createEvent(dto, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(agendaService.mapToDto(newEvent));
    }

    /**
     * Retorna todos os eventos da agenda do usuário autenticado.
     *
     * @param authentication Dados do usuário autenticado
     * @return Lista de eventos do usuário
     */
    @Operation(summary = "Lista todos os eventos do usuário", description = "Retorna todos os eventos e visitas agendadas para o usuário autenticado.")
    @GetMapping("/eventos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AgendaResponseDTO>> getAllEvents(Authentication authentication) {
        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).user();
        List<AgendaResponseDTO> allEvents = agendaService.findAllEventsForUser(currentUser);
        return ResponseEntity.ok(allEvents);
    }

    /**
     * Atualiza um evento existente na agenda.
     *
     * @param id             ID do evento a ser atualizado
     * @param dto            Novos dados do evento
     * @param authentication Dados do usuário autenticado
     * @return Dados do evento atualizado
     */
    @Operation(summary = "Atualiza um evento", description = "Atualiza os dados de um evento existente na agenda do usuário autenticado.")
    @PutMapping("/eventos/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AgendaResponseDTO> updateEvent(
            @PathVariable Long id,
            @RequestBody @Valid CreateEventDTO dto,
            Authentication authentication) {

        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).user();
        AgendaEvent updatedEvent = agendaService.updateEvent(id, dto, currentUser);
        return ResponseEntity.ok(agendaService.mapToDto(updatedEvent));
    }

    /**
     * Reagenda uma visita técnica para nova data/horário.
     *
     * @param visitId        ID da visita a ser reagendada
     * @param dto            Dados do novo agendamento
     * @param authentication Dados do usuário autenticado
     * @return Dados da visita reagendada
     */
    @Operation(summary = "Reagenda uma visita", description = "Altera a data e/ou o turno de uma visita técnica agendada.")
    @PutMapping("/visitas/{visitId}/reagendar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> rescheduleVisit(
            @PathVariable Long visitId,
            @RequestBody @Valid RescheduleVisitDTO dto,
            Authentication authentication) {

        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).user();

        // [3] O Service agora retorna void (pois cria histórico + move visita)
        agendaService.rescheduleVisit(visitId, dto, currentUser);

        return ResponseEntity.ok().build();
    }

    /**
     * Remove um evento da agenda.
     *
     * @param id             ID do evento a ser removido
     * @param authentication Dados do usuário autenticado
     * @return 204 No Content
     */
    @Operation(summary = "Remove um evento", description = "Exclui permanentemente um evento da agenda do usuário.")
    @DeleteMapping("/eventos/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable Long id,
            Authentication authentication) {

        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).user();
        agendaService.deleteEvent(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retorna todos os eventos para administradores.
     * Permite filtrar por ID do usuário.
     *
     * @param userId ID do usuário para filtrar (opcional)
     * @return Lista de todos os eventos
     */
    @Operation(summary = "Lista todos os eventos (Admin)", description = "Retorna todos os eventos cadastrados no sistema, permitindo filtrar por usuário.")
    @GetMapping("/eventos/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AgendaResponseDTO>> getAllEventsForAdmin(
            @RequestParam(required = false) Long userId
    ) {
        List<AgendaResponseDTO> allEvents = agendaService.findAllEventsForAdmin(userId);
        return ResponseEntity.ok(allEvents);
    }

    /**
     * Endpoint específico para validar se o relatório pode ser enviado.
     * Deve ser chamado pelo Frontend ANTES de abrir a tela de assinatura ou enviar os dados.
     */
    @Operation(summary = "Valida submissão de relatório", description = "Verifica se o relatório de uma visita pode ser enviado com base na data e turno.")
    @GetMapping("/validate-report")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> validateReportSubmission(
            Authentication auth,
            @RequestParam Long visitId,
            @RequestParam LocalDate date,
            @RequestParam String shift
    ) {
        User user = ((CustomUserDetails) auth.getPrincipal()).user();

        try {
            // Chama a validação criada no Service
            agendaService.validateReportSubmission(visitId, user, date, shift);

            // Se passar sem erro, retorna 200 OK
            return ResponseEntity.ok().build();

        } catch (IllegalStateException e) {
            // Se houver conflito (bloqueio), retorna 409 Conflict com a mensagem do Service
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            // Erro de dados (ex: turno inválido)
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }


    /**
     * Retorna a disponibilidade de agenda do usuário para um determinado mês.
     * Este endpoint é utilizado para visualizar os dias disponíveis e ocupados em um calendário mensal.
     *
     * @param auth  Dados de autenticação do usuário atual
     * @param year  Ano para consulta da disponibilidade
     * @param month Mês para consulta da disponibilidade (1-12)
     * @return Lista de disponibilidade diária contendo informações sobre os horários livres e ocupados
     */
    @Operation(summary = "Consulta disponibilidade mensal", description = "Retorna a disponibilidade diária (livre/ocupado) de um usuário para um mês específico.")
    @GetMapping("/availability")
    public ResponseEntity<List<MonthlyAvailabilityDTO>> getAvailability(
            Authentication auth,
            @RequestParam int year,
            @RequestParam int month) {

        User user = ((CustomUserDetails) auth.getPrincipal()).user();
        return ResponseEntity.ok(agendaService.getMonthAvailability(user, year, month));
    }

    @Operation(summary = "Consulta agenda global", description = "Retorna todos os eventos de todos os técnicos em um período determinado.")
    @GetMapping("/global")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AgendaResponseDTO>> getGlobalAgenda(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<AgendaResponseDTO> globalEvents = agendaService.getGlobalEvents(startDate, endDate);
        return ResponseEntity.ok(globalEvents);
    }

    /**
     * Verifica se há conflito GLOBAL (aviso de outros técnicos).
     * Retorna 200 OK com mensagem de aviso se houver conflito, ou 200 OK vazio se estiver livre.
     */
    @Operation(summary = "Verifica conflitos globais", description = "Verifica se há outros técnicos agendados para a mesma data e turno para evitar conflitos.")
    @GetMapping("/check-global-conflicts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> checkGlobalConflicts(
            Authentication auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String shift
    ) {
        User user = ((CustomUserDetails) auth.getPrincipal()).user();
        String warningMessage = agendaService.checkGlobalConflicts(date, shift, user);

        if (warningMessage != null) {
            return ResponseEntity.ok(Map.of("warning", warningMessage));
        }
        return ResponseEntity.ok(Map.of());
    }

    /**
     * Exporta um documento PDF contendo os detalhes da agenda para um intervalo de datas especificado.
     *
     * @param startDate a data inicial do período da agenda, formatada como data ISO
     * @param endDate a data final do período da agenda, formatada como data ISO
     * @return um ResponseEntity contendo o PDF gerado como um array de bytes,
     *         com o tipo de conteúdo e os cabeçalhos adequados para download do arquivo
     */
    @Operation(summary = "Exporta agenda em PDF", description = "Gera um documento PDF com os eventos da agenda em um intervalo de datas.")
    @GetMapping("/export/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportAgendaPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // 1. Busca os dados
        List<AgendaResponseDTO> listaEventos = agendaService.getReportData(startDate, endDate);

        // 2. Prepara os dados para o ReportService
        Map<String, Object> data = new HashMap<>();

        // Dados principais
        data.put("itens", listaEventos);

        // Formata o período para exibir no título
        String periodoTexto = startDate.format(DateTimeFormatter.ofPattern("MM/yyyy")) +
                " a " + endDate.format(DateTimeFormatter.ofPattern("MM/yyyy"));
        data.put("periodo", periodoTexto);

        // O ReportService vai injetar automaticamente:
        // generatingCompanyName, generatingCompanyCnpj e generatingCompanyLogo (Base64)

        // 3. Gera o PDF usando o template "relatorio-agenda"
        byte[] pdfBytes = reportService.generatePdfFromHtml("relatorio-agenda", data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=agenda_visitas.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    /**
     * Confirma uma visita agendada para o ID de visita fornecido.
     * Garante que o usuário atualmente autenticado esteja autorizado a confirmar a visita.
     *
     * @param visitId o ID da visita a ser confirmada
     * @param authentication o token de autenticação que representa o usuário atualmente autenticado
     * @return um ResponseEntity com status HTTP 200 (OK) se a visita for confirmada com sucesso
     */
    @Operation(summary = "Confirma uma visita", description = "Marca uma visita agendada como confirmada pelo técnico responsável.")
    @PutMapping("/visitas/{visitId}/confirmar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> confirmVisit(
            @PathVariable Long visitId,
            Authentication authentication) {

        // Extrai o utilizador atual do token de autenticação
        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).user();

        // Passa o utilizador para o serviço validar
        agendaService.confirmVisit(visitId, currentUser);

        return ResponseEntity.ok().build();
    }
}