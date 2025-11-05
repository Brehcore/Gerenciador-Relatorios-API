package com.gotree.API.controllers;

import com.gotree.API.entities.Physiotherapist;
import com.gotree.API.repositories.PhysiotherapistRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/physiotherapists")
public class PhysiotherapistController {

    private final PhysiotherapistRepository repository;

    public PhysiotherapistController(PhysiotherapistRepository repository) {
        this.repository = repository;
    }

    // ENDPOINT PARA O DROPDOWN:
    // Qualquer usuário logado pode ver a lista de fisios
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Physiotherapist>> getAllPhysiotherapists() {
        return ResponseEntity.ok(repository.findAll());
    }

    // ENDPOINT PARA O CADASTRO (Criar novo fisio)
    // Protegido - Apenas ADMIN pode cadastrar
    @PostMapping
    public ResponseEntity<Physiotherapist> createPhysiotherapist(@Valid @RequestBody Physiotherapist physio) {
        // Você pode adicionar validação aqui (ex: verificar se CREFITO já existe)
        Physiotherapist savedPhysio = repository.save(physio);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedPhysio);
    }

    // (Opcional) Adicionar endpoints PUT e DELETE para gerenciar a lista
}