package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.agenda.AgendaResponseDTO;
import com.gotree.API.dto.agenda.CreateEventDTO;
import com.gotree.API.dto.agenda.MonthlyAvailabilityDTO;
import com.gotree.API.dto.agenda.ReportNotRealizedDTO;
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
import org.springframework.web.util.HtmlUtils;

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
     * @param shift Turno (MANHÃ ou TARDE)
     * @return 200 OK se disponível, 409 Conflict se indisponível
     */
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

    /**
     * Cria um novo evento na agenda do usuário.
     *
     * @param dto            Dados do evento a ser criado
     * @param authentication Dados do usuário autenticado
     * @return Dados do evento criado
     */
    @Operation(summary = "Cria um novo evento", description = "Cria um novo evento ou agendamento de visita na agenda do usuário autenticado.")
    @PostMapping("/eventos")
    @PreAuthorize("hasAuthority('CREATE_AGENDA') or hasRole('ADMIN')")
    public ResponseEntity<AgendaResponseDTO> createEvent(
            @RequestBody @Valid CreateEventDTO dto,
            Authentication authentication
    ) {
        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).user();

        CreateEventDTO cleanDto = new CreateEventDTO();

        if (dto.getTitle() != null) {
            cleanDto.setTitle(HtmlUtils.htmlEscape(dto.getTitle()));
        }
        if (dto.getDescription() != null) {
            cleanDto.setDescription(HtmlUtils.htmlEscape(dto.getDescription()));
        }
        if (dto.getEventType() != null) {
            cleanDto.setEventType(HtmlUtils.htmlEscape(dto.getEventType()));
        }
        if (dto.getShift() != null) {
            cleanDto.setShift(HtmlUtils.htmlEscape(dto.getShift()));
        }
        if (dto.getClientName() != null) {
            cleanDto.setClientName(HtmlUtils.htmlEscape(dto.getClientName()));
        }
        if (dto.getManualObservation() != null) {
            cleanDto.setManualObservation(HtmlUtils.htmlEscape(dto.getManualObservation()));
        }

        cleanDto.setEventDate(dto.getEventDate());

        AgendaEvent newEvent = agendaService.createEvent(cleanDto, currentUser);

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

    /**
     * Atualiza um evento existente na agenda.
     *
     * @param id             ID do evento a ser atualizado
     * @param dto            Novos dados do evento
     * @param authentication Dados do usuário autenticado
     * @return Dados do evento atualizado
     */
    @Operation(summary = "Atualiza um evento", description = "Atualiza os dados de um evento manual existente na agenda do usuário autenticado.")
    @PutMapping("/eventos/{id}")
    @PreAuthorize("hasAuthority('EDIT_AGENDA') or hasRole('ADMIN')")
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
     * @param dto            Dados do novo agendamento
     * @param authentication Dados do usuário autenticado
     * @return Dados da visita reagendada
     */
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

    /**
     * Remove um evento da agenda.
     *
     * @param id             ID do evento a ser removido
     * @param authentication Dados do usuário autenticado
     * @return 204 No Content
     */
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

    /**
     * Verifica se há conflito GLOBAL (aviso de outros técnicos).
     * Retorna 200 OK com mensagem de aviso se houver conflito, ou 200 OK vazio se estiver livre.
     */
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

    /**
     * Exporta um documento PDF contendo os detalhes da agenda para um intervalo de datas especificado.
     *
     * @param startDate a data inicial do período da agenda, formatada como data ISO
     * @param endDate a data final do período da agenda, formatada como data ISO
     * @return um ResponseEntity contendo o PDF gerado como um array de bytes,
     *         com o tipo de conteúdo e os cabeçalhos adequados para download do arquivo
     */
    @Operation(summary = "Exporta agenda em PDF", description = "Gera um documento PDF com os eventos da agenda, permitindo filtros por data, colaborador, tipo e empresa.")
    @GetMapping("/export/pdf")
    @PreAuthorize("hasAuthority('VIEW_AGENDA') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportAgendaPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String companyName) { // <-- NOVO PARÂMETRO

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