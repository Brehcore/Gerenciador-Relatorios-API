package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.visit.CreateTechnicalVisitRequestDTO;
import com.gotree.API.dto.visit.TechnicalVisitResponseDTO;
import com.gotree.API.entities.TechnicalVisit;
import com.gotree.API.entities.User;
import com.gotree.API.mappers.TechnicalVisitMapper;
import com.gotree.API.services.TechnicalVisitService;
import jakarta.validation.Valid;
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
import java.util.Map;

@RestController
@RequestMapping("/technical-visits")
public class TechnicalVisitController {

    private final TechnicalVisitService technicalVisitService;
    private final TechnicalVisitMapper technicalVisitMapper;

    public TechnicalVisitController(TechnicalVisitService technicalVisitService, TechnicalVisitMapper technicalVisitMapper) {
        this.technicalVisitService = technicalVisitService;
        this.technicalVisitMapper = technicalVisitMapper;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createVisit(@RequestBody @Valid CreateTechnicalVisitRequestDTO dto, Authentication authentication) {
        // Obtém os detalhes do utilizador autenticado de forma segura.
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User technician = userDetails.getUser();

        // Delega toda a lógica de negócio (criação, geração de PDF, salvamento) para o serviço.
        TechnicalVisit createdVisit = technicalVisitService.createAndGeneratePdf(dto, technician);

        // Retorna uma resposta de sucesso para o frontend com uma mensagem e o ID da visita criada.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Relatório de visita técnica criado com sucesso!",
                        "visitId", createdVisit.getId()
                ));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TechnicalVisitResponseDTO>> findMyVisits(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User technician = userDetails.getUser();

        List<TechnicalVisit> visits = technicalVisitService.findAllByTechnician(technician);

        List<TechnicalVisitResponseDTO> responseDtos = technicalVisitMapper.toDtoList(visits);

        return ResponseEntity.ok(responseDtos);
    }
}
