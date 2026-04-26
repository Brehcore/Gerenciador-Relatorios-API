package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.aep.AepDetailDTO;
import com.gotree.API.dto.aep.AepRequestDTO;
import com.gotree.API.entities.AepReport;
import com.gotree.API.entities.User;
import com.gotree.API.services.AepService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*; // Importe o PathVariable

import java.util.Map;

/**
 * Controlador REST responsável por gerenciar operações relacionadas a Avaliação Ergonômica Preliminar (AEPs).
 * Fornece endpoints para criar, atualizar e recuperar informações de AEPs.
 */
@Tag(name = "Avaliação Ergonômica Preliminar", description = "Criação de documentos que avaliam a eficácia profissional no ambiente de trabalho.")
@RestController
@RequestMapping("/aep-reports")
public class AepController {

    private final AepService aepService;

    public AepController(AepService aepService) {
        this.aepService = aepService;
    }

    /**
     * Cria uma nova Avaliação Ergonômica Preliminar (AEP).
     *
     * @param dto            - Objeto contendo os dados necessários para criar uma nova Avaliação Ergonômica Preliminar
     * @param authentication - Objeto de autenticação para identificar o usuário que está criando a AEP
     * @return ResponseEntity contendo a mensagem de sucesso e o ID da Avaliação criada
     */
    @Operation(summary = "Cria uma AEP", description = "Utiliza autenticação para identificar qual usuário está criando a AEP")
    @PostMapping
    @PreAuthorize("hasAuthority('EMIT_REPORTS') or hasRole('ADMIN')")
    public ResponseEntity<?> createAepReport(@RequestBody @Valid AepRequestDTO dto, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User evaluator = userDetails.user();

        // Chama o serviço para SALVAR os dados (sem ID existente)
        AepReport createdAep = aepService.saveAepData(dto, evaluator, null);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "AEP salva com sucesso!",
                        "reportId", createdAep.getId()
                ));
    }

    /**
     * Atualiza uma Avaliação Ergonômica Preliminar (AEP) existente.
     *
     * @param id             - ID da Avaliação que deve ser atualizada
     * @param dto            - Dados atualizados para a Avaliação Ergonômica Preliminar
     * @param authentication - Objeto de autenticação para identificar o usuário atual
     * @return ResponseEntity contendo a mensagem de sucesso e o ID da Avaliação atualizada
     */
    @Operation(summary = "Atualiza uma AEP", description = "Utiliza autenticação do usuário e ID da AEP existente para atualização.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDIT_REPORTS') or hasRole('ADMIN')")
    public ResponseEntity<?> updateAepReport(@PathVariable Long id, @RequestBody @Valid AepRequestDTO dto, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User evaluator = userDetails.user();

        // Chama o serviço para ATUALIZAR os dados (com ID existente)
        AepReport updatedAep = aepService.saveAepData(dto, evaluator, id);

        return ResponseEntity
                .ok()
                .body(Map.of(
                        "message", "AEP atualizada com sucesso!",
                        "reportId", updatedAep.getId()
                ));
    }

    /**
     * Recupera os detalhes de uma Avaliação Ergonômica Preliminar (AEP) específica.
     *
     * @param id             - ID da Avaliação a ser consultada
     * @param authentication - Objeto de autenticação que identifica o usuário solicitante
     * @return ResponseEntity contendo os detalhes da Avaliação solicitada
     */
    @Operation(summary = "Recupera os detalhes de uma AEP", description = "Utiliza autenticação do usuário e ID da AEP existente pegando os detalhes que estavam previamente selecionados.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_REPORTS') or hasRole('ADMIN')")
    public ResponseEntity<AepDetailDTO> getAepReportDetails(@PathVariable Long id, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User currentUser = userDetails.user();

        AepDetailDTO aepDetails = aepService.findAepDetails(id, currentUser);

        return ResponseEntity.ok(aepDetails);
    }

}