package com.gotree.API.controllers;

import com.gotree.API.services.SectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST responsável por gerenciar operações relacionadas a setores.
 * Fornece endpoints para criação, leitura, atualização e exclusão de setores.
 */
@Tag(name = "Setores", description = "Gerenciamento de setores")
@RestController
@RequestMapping("/sectors")
public class SectorController {

    private final SectorService sectorService;

    public SectorController(SectorService sectorService) {
        this.sectorService = sectorService;
    }

    /**
     * Remove um setor do sistema.
     *
     * @param id identificador único do setor a ser removido
     * @return ResponseEntity sem conteúdo (204) em caso de sucesso,
     * ResponseEntity com status 409 (CONFLICT) se o setor possuir dependências,
     * ResponseEntity com status 404 (NOT_FOUND) se o setor não for encontrado
     */
    @Operation(summary = "Remove um setor", description = "Remove um setor do sistema")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_COMPANIES') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteSector(@PathVariable Long id) {
        try {
            sectorService.deleteSector(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }
}