package com.gotree.API.modules.administration.presentation.controllers;

import com.gotree.API.modules.administration.application.services.JobRoleService;
import com.gotree.API.modules.administration.presentation.dto.JobRoleDTO;
import com.gotree.API.modules.administration.presentation.dto.JobRoleResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Cargos", description = "Gerenciamento de cargos")
@RestController
@RequestMapping("/job-roles")
public class JobRoleController {

    private final JobRoleService jobRoleService;

    public JobRoleController(JobRoleService jobRoleService) {
        this.jobRoleService = jobRoleService;
    }

    @Operation(summary = "Recupera todos os cargos", description = "Recupera todos os cargos de uma empresa específica, ordenados por nome e em ordem alfabética.")
    @GetMapping("/company/{companyId}")
    @PreAuthorize("hasAuthority('VIEW_COMPANIES') or hasRole('ADMIN')")
    public ResponseEntity<List<JobRoleResponseDTO>> getByCompany(@PathVariable Long companyId) {
        return ResponseEntity.ok(jobRoleService.getByCompany(companyId));
    }

    @Operation(summary = "Criação de cargo", description = "Cria um novo cargo para uma empresa específica.")
    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_COMPANIES') or hasRole('ADMIN')")
    public ResponseEntity<JobRoleResponseDTO> create(@RequestBody @Valid JobRoleDTO dto) {
        JobRoleResponseDTO response = jobRoleService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //TODO: Criar endpoints para editar e excluir cargos
}