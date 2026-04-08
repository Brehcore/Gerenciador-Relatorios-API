package com.gotree.API.controllers;

import com.gotree.API.dto.physiotherapist.CreatePhysiotherapistDTO;
import com.gotree.API.entities.Physiotherapist;
import com.gotree.API.repositories.PhysiotherapistRepository;
import com.gotree.API.utils.XmlSanitizer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Fisioterapeutas", description = "Gerenciamento de fisioterapeutas")
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
    @Operation(summary = "Lista todos os fisioterapeutas", description = "Retorna a lista de todos os fisioterapeutas cadastrados no sistema.")
    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_REPORTS') or hasRole('ADMIN')")
    public ResponseEntity<List<Physiotherapist>> getAllPhysiotherapists() {
        return ResponseEntity.ok(repository.findAll());
    }


    /**
     * Cria um novo fisioterapeuta no sistema.
     * Este endpoint recebe os dados de um novo fisioterapeuta através do DTO,
     * sanitiza os campos para prevenir ataques XSS e persiste o registro na base de dados.
     * O processo garante que apenas dados seguros sejam armazenados no sistema.
     *
     * @param dto Objeto contendo os dados do fisioterapeuta a ser criado (nome e CREFITO)
     * @return ResponseEntity contendo o fisioterapeuta criado com status HTTP 201 (CREATED)
     * @security Endpoint requer autenticação
     */
    @Operation(summary = "Cria um novo fisioterapeuta", description = "Cria um novo registo de fisioterapeuta no sistema de forma segura.")
    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_REPORTS') or hasRole('ADMIN')")
    public ResponseEntity<Physiotherapist> createPhysiotherapist(@Valid @RequestBody CreatePhysiotherapistDTO dto) {

        // 1. Instancia uma Entidade NOVA (A IDE confia nisto, pois o objeto nasceu no servidor)
        Physiotherapist physio = new Physiotherapist();

        // 2. Transfere os dados do DTO para a Entidade, SANITIZANDO no processo
        physio.setName(XmlSanitizer.sanitize(dto.getName()));
        physio.setCrefito(XmlSanitizer.sanitize(dto.getCrefito()));

        // 3. Guarda na base de dados (100% livre de XSS)
        Physiotherapist savedPhysio = repository.save(physio);

        // 4. Retorna a entidade guardada
        return ResponseEntity.status(HttpStatus.CREATED).body(savedPhysio);
    }

    // TODO: Adicionar endpoints PUT e DELETE para gerir a lista
}