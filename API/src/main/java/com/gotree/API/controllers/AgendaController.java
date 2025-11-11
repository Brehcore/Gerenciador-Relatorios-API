package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.agenda.AgendaResponseDTO;
import com.gotree.API.dto.agenda.CreateEventDTO;
import com.gotree.API.dto.agenda.RescheduleVisitDTO;
import com.gotree.API.entities.AgendaEvent;
import com.gotree.API.entities.User;
import com.gotree.API.services.AgendaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agenda")
public class AgendaController {

    private final AgendaService agendaService;

    public AgendaController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    @PostMapping("/eventos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AgendaResponseDTO> createEvent(
            @RequestBody @Valid CreateEventDTO dto,
            Authentication authentication
    ) {
        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        AgendaEvent newEvent = agendaService.createEvent(dto, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(agendaService.mapToDto(newEvent));
    }

    @GetMapping("/eventos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AgendaResponseDTO>> getAllEvents(Authentication authentication) {
        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        List<AgendaResponseDTO> allEvents = agendaService.findAllEventsForUser(currentUser);
        return ResponseEntity.ok(allEvents);
    }

    /**
     * Atualiza um evento GENÉRICO (ex.: reunião).
     */
    @PutMapping("/eventos/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AgendaResponseDTO> updateEvent(
            @PathVariable Long id, // ID do AgendaEvent
            @RequestBody @Valid CreateEventDTO dto,
            Authentication authentication) {

        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        AgendaEvent updatedEvent = agendaService.updateEvent(id, dto, currentUser);
        return ResponseEntity.ok(agendaService.mapToDto(updatedEvent));
    }

    /**
     * (NOVO) Reagenda uma "Próxima Visita" (converte em um AgendaEvent).
     */
    @PutMapping("/visitas/{visitId}/reagendar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AgendaResponseDTO> rescheduleVisit(
            @PathVariable Long visitId, // ID do TechnicalVisit original
            @RequestBody @Valid RescheduleVisitDTO dto,
            Authentication authentication) {

        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        AgendaEvent rescheduledEvent = agendaService.rescheduleVisit(visitId, dto, currentUser);
        return ResponseEntity.ok(agendaService.mapToDto(rescheduledEvent));
    }

    /**
     * Deleta um evento (genérico OU reagendado).
     */
    @DeleteMapping("/eventos/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable Long id, // ID do AgendaEvent
            Authentication authentication) {

        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        agendaService.deleteEvent(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint de Admin para ver TODOS os compromissos.
     */
    @GetMapping("/eventos/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AgendaResponseDTO>> getAllEventsForAdmin() {
        List<AgendaResponseDTO> allEvents = agendaService.findAllEventsForAdmin();
        return ResponseEntity.ok(allEvents);
    }
}