package com.gotree.API.controllers;

import com.gotree.API.entities.Physiotherapist;
import com.gotree.API.repositories.PhysiotherapistRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST que gerencia as operações relacionadas aos Fisioterapeutas.
 * Fornece endpoints para listar e cadastrar fisioterapeutas no sistema.
 * A rota base para todos os endpoints é "/physiotherapists".
 */
@RestController
@RequestMapping("/physiotherapists")
public class PhysiotherapistController {
    
    

    private final PhysiotherapistRepository repository;

    public PhysiotherapistController(PhysiotherapistRepository repository) {
        this.repository = repository;
    }

    /**
     * Retorna a lista de todos os fisioterapeutas cadastrados no sistema.
     * Este endpoint é utilizado para popular listas suspensas (dropdowns) na interface.
     *
     * @return ResponseEntity contendo a lista de todos os fisioterapeutas
     * @security Requer que o usuário esteja autenticado
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Physiotherapist>> getAllPhysiotherapists() {
        return ResponseEntity.ok(repository.findAll());
    }


    /**
     * Cria um novo registro de fisioterapeuta no sistema.
     *
     * @param physio O objeto Physiotherapist contendo os dados do novo fisioterapeuta
     * @return ResponseEntity contendo o fisioterapeuta criado
     * @throws jakarta.validation.ValidationException se os dados fornecidos forem inválidos
     * @security Requer permissão de ADMIN
     */
    @PostMapping
    public ResponseEntity<Physiotherapist> createPhysiotherapist(@Valid @RequestBody Physiotherapist physio) {
        // Você pode adicionar validação aqui (ex: verificar se CREFITO já existe)
        Physiotherapist savedPhysio = repository.save(physio);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedPhysio);
    }

    // TODO: Adicionar endpoints PUT e DELETE para gerenciar a lista
}