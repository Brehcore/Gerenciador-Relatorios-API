package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.aep.AepDetailDTO;
import com.gotree.API.dto.aep.AepRequestDTO;
import com.gotree.API.entities.AepReport;
import com.gotree.API.entities.User;
import com.gotree.API.services.AepService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*; // Importe o PathVariable

import java.util.Map;

@RestController
@RequestMapping("/aep-reports")
public class AepController {

    private final AepService aepService;

    public AepController(AepService aepService) {
        this.aepService = aepService;
    }

    // CRIA uma nova AEP (só salva no banco)
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createAepReport(@RequestBody @Valid AepRequestDTO dto, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User evaluator = userDetails.getUser();

        // Chama o serviço para SALVAR os dados (sem ID existente)
        AepReport createdAep = aepService.saveAepData(dto, evaluator, null);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "AEP salva com sucesso!",
                        "reportId", createdAep.getId()
                ));
    }

    // EDITA uma AEP existente (salva no banco e apaga PDF antigo)
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateAepReport(@PathVariable Long id, @RequestBody @Valid AepRequestDTO dto, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User evaluator = userDetails.getUser();

        // Chama o serviço para ATUALIZAR os dados (com ID existente)
        AepReport updatedAep = aepService.saveAepData(dto, evaluator, id);

        return ResponseEntity
                .ok()
                .body(Map.of(
                        "message", "AEP atualizada com sucesso!",
                        "reportId", updatedAep.getId()
                ));
    }

    // --- NOVO ENDPOINT PARA CARREGAR DADOS PARA EDIÇÃO ---
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AepDetailDTO> getAepReportDetails(@PathVariable Long id, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User currentUser = userDetails.getUser();

        AepDetailDTO aepDetails = aepService.findAepDetails(id, currentUser);

        return ResponseEntity.ok(aepDetails);
    }

}