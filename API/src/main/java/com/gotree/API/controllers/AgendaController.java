package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.agenda.AgendaResponseDTO;
import com.gotree.API.dto.agenda.CreateEventDTO;
import com.gotree.API.entities.AgendaEvent;
import com.gotree.API.entities.User;
import com.gotree.API.services.AgendaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agenda")
public class AgendaController {

    private final AgendaService agendaService;

    public AgendaController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }
    /**
     * Endpoint para CRIAR novos eventos (Integração, Reunião, etc.)
     */
    @PostMapping("/eventos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AgendaEvent> createEvent(
            @RequestBody CreateEventDTO dto, // Um DTO simples com title, description, eventDate
            Authentication authentication
    ) {
        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        AgendaEvent newEvent = agendaService.createEvent(dto, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(newEvent);
    }

    /**
     * Endpoint para LER TODOS os eventos e visitas
     * Este é o endpoint que o calendário do front-end vai chamar!
     */
    @GetMapping("/eventos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AgendaResponseDTO>> getAllEvents(Authentication authentication) {
        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).getUser();

        // O AgendaService terá a lógica de juntar tudo
        List<AgendaResponseDTO> allEvents = agendaService.findAllEventsForUser(currentUser);

        return ResponseEntity.ok(allEvents);
    }

    // Criar endpoint de editar e excluir eventos
}