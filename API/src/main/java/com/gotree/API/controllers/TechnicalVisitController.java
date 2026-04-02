package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.visit.CreateTechnicalVisitRequestDTO;
import com.gotree.API.dto.visit.TechnicalVisitResponseDTO;
import com.gotree.API.entities.TechnicalVisit;
import com.gotree.API.entities.User;
import com.gotree.API.mappers.TechnicalVisitMapper;
import com.gotree.API.services.TechnicalVisitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controller responsável por gerenciar as operações relacionadas às visitas técnicas.
 * Fornece endpoints para:
 * - Criar novas visitas técnicas
 * - Listar visitas técnicas do técnico autenticado
 * - Verificar disponibilidade de agenda para próximas visitas
 * - Assinar digitalmente visitas técnicas via certificado ICP-Brasil
 */
@Tag(name = "Visitas Técnicas", description = "Controller responsável por gerenciar as operações relacionadas às visitas técnicas")
@RestController
@RequestMapping("/technical-visits")
public class TechnicalVisitController {

    private final TechnicalVisitService technicalVisitService;
    private final TechnicalVisitMapper technicalVisitMapper;

    public TechnicalVisitController(TechnicalVisitService technicalVisitService, TechnicalVisitMapper technicalVisitMapper) {
        this.technicalVisitService = technicalVisitService;
        this.technicalVisitMapper = technicalVisitMapper;
    }

    /**
     * Cria uma nova visita técnica e gera o relatório PDF correspondente.
     * @param dto            Objeto contendo os dados necessários para criação da visita técnica
     * @param authentication Informações de autenticação do técnico que está criando a visita
     * @return ResponseEntity com status 201 (CREATED) contendo mensagem de sucesso e o ID da visita criada
     */
    @Operation(summary = "Cria uma visita", description = "Criação de uma nova visita técnica.")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createVisit(@RequestBody @Valid CreateTechnicalVisitRequestDTO dto, Authentication authentication) {
        // Obtém os detalhes do utilizador autenticado de forma segura.
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User technician = userDetails.user();

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

    /**
     * Retorna todas as visitas técnicas realizadas pelo técnico autenticado.
     * @param authentication Informações de autenticação do técnico logado
     * @return ResponseEntity com status 200 (OK) contendo a lista de DTOs das visitas técnicas do técnico
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TechnicalVisitResponseDTO>> findMyVisits(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User technician = userDetails.user();

        List<TechnicalVisit> visits = technicalVisitService.findAllByTechnician(technician);

        List<TechnicalVisitResponseDTO> responseDtos = technicalVisitMapper.toDtoList(visits);

        return ResponseEntity.ok(responseDtos);
    }

    /**
     * Verifica a disponibilidade do técnico para uma próxima visita em uma data e turno específicos.
     * Este endpoint valida se já existe alguma visita técnica agendada para a data e turno informados
     * antes de permitir um novo agendamento. Também retorna avisos sobre outros técnicos agendados
     * no mesmo dia e turno.
     * @param auth  Informações de autenticação do técnico que está verificando disponibilidade
     * @param date  Data proposta para a próxima visita (formato: yyyy-MM-dd)
     * @param shift Turno proposto para a próxima visita (valores aceitos: MORNING, AFTERNOON)
     * @return ResponseEntity com status 200 (OK) contendo informações de disponibilidade se livre,
     *         ou status 409 (CONFLICT) contendo detalhes do bloqueio se já houver agendamento
     */
    @Operation(summary = "Verificar Disponibilidade", description = "Verifica se o técnico tem a agenda livre e retorna avisos de outros técnicos no mesmo dia.")
    @GetMapping("/check-availability")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> checkNextVisitAvailability(
            Authentication auth,
            @RequestParam LocalDate date,
            @RequestParam String shift
    ) {
        User currentUser = ((CustomUserDetails) auth.getPrincipal()).user();

        Map<String, Object> validationResult = technicalVisitService.validateNextVisitSchedule(date, shift, currentUser);

        if ((Boolean) validationResult.get("blocked")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(validationResult);
        }

        return ResponseEntity.ok(validationResult);
    }

    /**
     * Assina digitalmente uma visita técnica existente utilizando certificado digital ICP-Brasil.
     * Este endpoint permite que o técnico autenticado assine digitalmente uma visita técnica previamente criada,
     * garantindo a autenticidade e integridade do documento mediante certificação digital.
     * @param id             Identificador único da visita técnica a ser assinada
     * @param authentication Informações de autenticação do técnico que está assinando a visita
     * @return ResponseEntity com status 200 (OK) e mensagem de sucesso se a assinatura for bem-sucedida,
     *         status 400 (BAD REQUEST) se houver erro de validação ou estado inválido,
     *         ou status 500 (INTERNAL SERVER ERROR) se houver erro no processamento da assinatura
     */
    @Operation(summary = "Assinatura digital em um relatório", description = "Assina digitalmente um relatório de risco utilizando certificado digital padrão ICP-Brasil")
    @PostMapping("/{id}/sign")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> signVisit(@PathVariable Long id, Authentication authentication) {
        try {
            User user = ((CustomUserDetails) authentication.getPrincipal()).user();
            technicalVisitService.signExistingVisit(id, user);
            return ResponseEntity.ok(Map.of("message", "Visita Técnica assinada com sucesso via Certificado Digital."));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erro ao processar assinatura: " + e.getMessage()));
        }
    }
}
