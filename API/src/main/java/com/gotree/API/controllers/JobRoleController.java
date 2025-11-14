package com.gotree.API.controllers;

import com.gotree.API.dto.risk.JobRoleDTO;
import com.gotree.API.dto.risk.JobRoleResponseDTO;
import com.gotree.API.entities.Company;
import com.gotree.API.entities.JobRole;
import com.gotree.API.repositories.CompanyRepository;
import com.gotree.API.repositories.JobRoleRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller responsável por gerenciar operações relacionadas aos cargos (JobRole).
 * Fornece endpoints para criação e consulta de cargos por empresa.
 */
@RestController
@RequestMapping("/job-roles")
public class JobRoleController {

    private final JobRoleRepository jobRoleRepository;
    private final CompanyRepository companyRepository;

    public JobRoleController(JobRoleRepository jobRoleRepository, CompanyRepository companyRepository) {
        this.jobRoleRepository = jobRoleRepository;
        this.companyRepository = companyRepository;
    }

    /**
     * Recupera todos os cargos de uma empresa específica, ordenados por nome em ordem alfabética.
     *
     * @param companyId ID da empresa para filtrar os cargos
     * @return ResponseEntity contendo a lista de cargos da empresa
     * @throws java.util.NoSuchElementException se a empresa não for encontrada
     */
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<JobRoleResponseDTO>> getByCompany(@PathVariable Long companyId) {
        // 1. Busca a empresa
        Company company = companyRepository.findById(companyId).orElseThrow();

        // 2. Busca a lista de entidades (JobRole)
        List<JobRole> roles = jobRoleRepository.findByCompanyOrderByNameAsc(company);

        // 3. Converte a lista de Entidades para a lista de DTOs
        List<JobRoleResponseDTO> responseList = roles.stream()
                .map(role -> {
                    JobRoleResponseDTO dto = new JobRoleResponseDTO();
                    dto.setId(role.getId());
                    dto.setName(role.getName());
                    dto.setCompanyId(role.getCompany().getId());
                    dto.setCompanyName(role.getCompany().getName());
                    return dto;
                })
                .toList(); // (Ou .collect(Collectors.toList()) se estiver em Java < 16)

        return ResponseEntity.ok(responseList);
    }

    /**
     * Cria um novo cargo para uma empresa específica.
     *
     * @param dto DTO contendo as informações do cargo a ser criado
     * @return ResponseEntity contendo o cargo criado
     * @throws java.util.NoSuchElementException                 se a empresa não for encontrada
     */
    @PostMapping
    public ResponseEntity<JobRoleResponseDTO> create(@RequestBody @Valid JobRoleDTO dto) { // <-- Mude o retorno
        Company company = companyRepository.findById(dto.getCompanyId()).orElseThrow();

        if(jobRoleRepository.existsByNameAndCompany(dto.getName(), company)) {
            return ResponseEntity.badRequest().build();
        }

        JobRole role = new JobRole();
        role.setName(dto.getName());
        role.setCompany(company);

        JobRole savedRole = jobRoleRepository.save(role);

        // Mapeamento manual para o DTO de resposta
        JobRoleResponseDTO response = new JobRoleResponseDTO();
        response.setId(savedRole.getId());
        response.setName(savedRole.getName());
        response.setCompanyId(savedRole.getCompany().getId());
        response.setCompanyName(savedRole.getCompany().getName());

        return ResponseEntity.ok(response);
    }
}