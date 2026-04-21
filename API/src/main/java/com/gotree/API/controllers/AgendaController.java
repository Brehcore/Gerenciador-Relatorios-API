package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.agenda.AgendaResponseDTO;
import com.gotree.API.dto.agenda.CreateEventDTO;
import com.gotree.API.dto.agenda.MonthlyAvailabilityDTO;
import com.gotree.API.dto.agenda.ReportNotRealizedDTO;
import com.gotree.API.dto.agenda.RescheduleVisitDTO;
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
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Agenda e Visitas", description = "Gerenciamento do calendário e agendamento de eventos.")
@RestController
@RequestMapping(value = "/api/agenda", produces = MediaType.APPLICATION_JSON_VALUE)
public class AgendaController {

    private final AgendaService agendaService;
    private final ReportService reportService;

    public AgendaController(AgendaService agendaService, ReportService reportService) {
        this.agendaService = agendaService;
        this.reportService = reportService;
    }

    @Operation(summary = "Cria um novo evento")
    @PostMapping("/eventos")
    @PreAuthorize("hasAuthority('CREATE_AGENDA') or hasRole('ADMIN')")
    public ResponseEntity<AgendaResponseDTO> createEvent(
            @RequestBody @Valid CreateEventDTO dto,
            Authentication authentication
    ) {
        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).user();

        // O Service já devolve o DTO pronto!
        AgendaResponseDTO response = agendaService.createEvent(dto, currentUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Atualiza um evento")
    @PutMapping("/eventos/{id}")
    @PreAuthorize("hasAuthority('EDIT_AGENDA') or hasRole('ADMIN')")
    public ResponseEntity<AgendaResponseDTO> updateEvent(
            @PathVariable Long id,
            @RequestBody @Valid CreateEventDTO dto,
            Authentication authentication) {

        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).user();

        // O Service já devolve o DTO pronto!
        AgendaResponseDTO response = agendaService.updateEvent(id, dto, currentUser);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove um evento", description = "Exclui permanentemente um evento da agenda do usuário.")
    @DeleteMapping("/eventos/{id}")
    @PreAuthorize("hasAuthority('DELETE_AGENDA') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable Long id,
            Authentication authentication) {

        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).user();
        agendaService.deleteEvent(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Verifica disponibilidade", description = "Verifica se uma determinada data e turno estão disponíveis na agenda do usuário autenticado.")
    @GetMapping("/check-availability")
    @PreAuthorize("hasAuthority('VIEW_AGENDA') or hasRole('ADMIN')")
    public ResponseEntity<?> checkAvailability(
            Authentication auth,
            @RequestParam LocalDate date,
            @RequestParam String shift
    ) {
        User user = ((CustomUserDetails) auth.getPrincipal()).user();

        String warning = agendaService.checkAvailability(user, date, shift);

        if (warning != null) {
            // 409 Conflict: Informa o frontend que a agenda está cheia
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", HtmlUtils.htmlEscape(warning)));
        }

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Lista todos os eventos do usuário", description = "Retorna todos os eventos e visitas agendadas para o usuário autenticado.")
    @GetMapping("/eventos")
    @PreAuthorize("hasAuthority('VIEW_AGENDA') or hasRole('ADMIN')")
    public ResponseEntity<List<AgendaResponseDTO>> getAllEvents(Authentication authentication) {
        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).user();
        List<AgendaResponseDTO> allEvents = agendaService.findAllEventsForUser(currentUser);
        return ResponseEntity.ok(allEvents);
    }

    @Operation(summary = "Lista os próximos eventos", description = "Retorna os eventos do usuário a partir da data atual.")
    @GetMapping("/eventos/proximos")
    @PreAuthorize("hasAuthority('VIEW_AGENDA') or hasRole('ADMIN')")
    public ResponseEntity<List<AgendaResponseDTO>> getUpcomingEvents(Authentication authentication) {
        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).user();
        List<AgendaResponseDTO> upcomingEvents = agendaService.findUpcomingEventsForUser(currentUser);
        return ResponseEntity.ok(upcomingEvents);
    }

    @Operation(summary = "Reagenda uma visita", description = "Altera a data e/ou o turno de um evento na agenda.")
    @PutMapping("/{eventId}/reagendar")
    @PreAuthorize("hasAuthority('EDIT_AGENDA') or hasRole('ADMIN')")
    public ResponseEntity<Void> rescheduleVisit(
            @PathVariable Long eventId,
            @RequestBody @Valid RescheduleVisitDTO dto,
            Authentication authentication) {

        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).user();
        agendaService.rescheduleVisit(eventId, dto, currentUser);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Lista todos os eventos (Admin)", description = "Retorna todos os eventos cadastrados no sistema, permitindo filtrar por usuário.")
    @GetMapping("/eventos/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AgendaResponseDTO>> getAllEventsForAdmin(
            @RequestParam(required = false) Long userId
    ) {
        List<AgendaResponseDTO> allEvents = agendaService.findAllEventsForAdmin(userId);
        return ResponseEntity.ok(allEvents);
    }

    @Operation(summary = "Valida submissão de relatório", description = "Verifica se o relatório de uma visita pode ser enviado com base na data e turno.")
    @GetMapping("/validate-report")
    @PreAuthorize("hasAuthority('VIEW_AGENDA') or hasRole('ADMIN')")
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
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", HtmlUtils.htmlEscape(e.getMessage())));
        } catch (IllegalArgumentException e) {
            // Erro de dados (ex: turno inválido)
            return ResponseEntity.badRequest().body(Map.of("message", HtmlUtils.htmlEscape(e.getMessage())));
        }
    }


    @Operation(summary = "Consulta disponibilidade mensal", description = "Retorna a disponibilidade diária (livre/ocupado) de um usuário para um mês específico.")
    @GetMapping("/availability")
    @PreAuthorize("hasAuthority('VIEW_AGENDA') or hasRole('ADMIN')")
    public ResponseEntity<List<MonthlyAvailabilityDTO>> getAvailability(
            @RequestParam int year,
            @RequestParam int month) {

        return ResponseEntity.ok(agendaService.getMonthAvailability(year, month));
    }

    @Operation(summary = "Consulta agenda global", description = "Retorna todos os eventos de todos os técnicos em um período determinado.")
    @GetMapping("/global")
    @PreAuthorize("hasAuthority('VIEW_AGENDA') or hasRole('ADMIN')")
    public ResponseEntity<List<AgendaResponseDTO>> getGlobalAgenda(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<AgendaResponseDTO> globalEvents = agendaService.getGlobalEvents(startDate, endDate);
        return ResponseEntity.ok(globalEvents);
    }

    @Operation(summary = "Verifica conflitos globais", description = "Verifica se há outros técnicos agendados para a mesma data e turno para evitar conflitos.")
    @GetMapping("/check-global-conflicts")
    @PreAuthorize("hasAuthority('VIEW_AGENDA') or hasRole('ADMIN')")
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

    @Operation(summary = "Exporta agenda em PDF", description = "Gera um documento PDF com os eventos da agenda, permitindo filtros por data, colaborador, tipo e empresa.")
    @GetMapping("/export/pdf")
    @PreAuthorize("hasAuthority('VIEW_AGENDA') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportAgendaPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String companyName) {

        // 1. Busca os dados com TODOS os filtros
        List<AgendaResponseDTO> listaEventos = agendaService.getReportData(startDate, endDate, userId, eventType, companyName);

        // 2. Prepara os dados para o ReportService
        Map<String, Object> data = new HashMap<>();
        data.put("itens", listaEventos);

        String periodoTexto = startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                " a " + endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        data.put("periodo", periodoTexto);

        // 3. Envia os rótulos dos filtros para exibir no cabeçalho do PDF
        data.put("filtroTipo", (eventType != null && !eventType.isBlank()) ? eventType : "TODOS");
        data.put("filtroEmpresa", (companyName != null && !companyName.isBlank()) ? companyName.toUpperCase() : "TODAS");

        if (userId != null && !listaEventos.isEmpty()) {
            data.put("filtroColaborador", listaEventos.getFirst().getResponsibleName());
        } else if (userId != null) {
            data.put("filtroColaborador", "ID: " + userId + " (Sem eventos)");
        } else {
            data.put("filtroColaborador", "TODOS");
        }

        byte[] pdfBytes = reportService.generatePdfFromHtml("relatorio-agenda", data);

        return ResponseEntity.ok()
                // Atualizado o nome do arquivo baixado também
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=relatorio_agendamentos.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @Operation(summary = "Confirma uma visita", description = "Marca uma visita agendada como confirmada pelo técnico responsável.")
    @PutMapping("/visitas/{visitId}/confirmar")
    @PreAuthorize("hasAuthority('EDIT_AGENDA') or hasRole('ADMIN')")
    public ResponseEntity<Void> confirmVisit(
            @PathVariable Long visitId,
            Authentication authentication) {

        // Extrai o utilizador atual do token de autenticação
        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).user();

        // Passa o utilizador para o serviço validar
        agendaService.confirmVisit(visitId, currentUser);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/not-realized")
    @Operation(summary = "Reportar visita não realizada", description = "Marca um evento da agenda como não realizado e salva a justificativa do técnico.")
    @PreAuthorize("hasAuthority('CREATE_AGENDA') or hasRole('ADMIN')")
    public ResponseEntity<Void> reportVisitNotRealized(
            @PathVariable Long id,
            @Valid @RequestBody ReportNotRealizedDTO dto,
            Authentication authentication) {

        // Extrai o usuário igual aos outros endpoints!
        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).user();

        agendaService.reportVisitNotRealized(id, dto, currentUser);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/realized")
    @Operation(summary = "Marcar evento como realizado", description = "Dá baixa no evento da agenda indicando que ele ocorreu com sucesso.")
    @PreAuthorize("hasAuthority('EDIT_AGENDA') or hasRole('ADMIN')")
    public ResponseEntity<Void> markEventAsRealized(
            @PathVariable Long id,
            Authentication authentication) {

        // Extrai o usuário igual aos outros endpoints!
        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).user();

        agendaService.markEventAsRealized(id, currentUser);

        return ResponseEntity.noContent().build();
    }
}