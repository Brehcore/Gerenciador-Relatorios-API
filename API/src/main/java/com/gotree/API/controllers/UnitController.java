package com.gotree.API.controllers;

import com.gotree.API.services.UnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST responsável por gerenciar as operações relacionadas a unidades.
 * Fornece endpoints para manipulação de dados de unidades no sistema.
 */
@Tag(name = "Unidades", description = "Gerenciamento de unidades")
@RestController
@RequestMapping("/units")
public class UnitController {

    private final UnitService unitService;

    public UnitController(UnitService unitService) {
        this.unitService = unitService;
    }

    /**
     * Remove uma unidade do sistema através do seu identificador.
     *
     * @param id identificador único da unidade a ser removida
     * @return ResponseEntity sem conteúdo (204) se a exclusão for bem-sucedida,
     * ResponseEntity com status de conflito (409) se houver dependências que impedem a exclusão,
     * ResponseEntity com status não encontrado (404) se a unidade não existir
     */
    @Operation(summary = "Remove uma unidade", description = "Remove uma unidade do sistema")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUnit(@PathVariable Long id) {
        try {
            unitService.deleteUnit(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }
}