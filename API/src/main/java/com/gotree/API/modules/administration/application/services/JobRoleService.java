package com.gotree.API.modules.administration.application.services;

import com.gotree.API.modules.administration.domain.entities.Company;
import com.gotree.API.modules.administration.domain.entities.JobRole;
import com.gotree.API.modules.administration.infrastructure.repositories.CompanyRepository;
import com.gotree.API.modules.administration.infrastructure.repositories.JobRoleRepository;
import com.gotree.API.modules.administration.presentation.dto.JobRoleDTO;
import com.gotree.API.modules.administration.presentation.dto.JobRoleResponseDTO;
import com.gotree.API.modules.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class JobRoleService {

    private final JobRoleRepository jobRoleRepository;
    private final CompanyRepository companyRepository;

    public JobRoleService(JobRoleRepository jobRoleRepository, CompanyRepository companyRepository) {
        this.jobRoleRepository = jobRoleRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional(readOnly = true)
    public List<JobRoleResponseDTO> getByCompany(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada com o ID: " + companyId));

        return jobRoleRepository.findByCompanyOrderByNameAsc(company).stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    @Transactional
    public JobRoleResponseDTO create(JobRoleDTO dto) {
        Company company = companyRepository.findById(dto.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada com o ID: " + dto.getCompanyId()));

        // Validação de negócio centralizada!
        if (jobRoleRepository.existsByNameAndCompany(dto.getName(), company)) {
            throw new IllegalStateException("A empresa já possui o cargo: " + dto.getName());
        }

        JobRole role = new JobRole();
        role.setName(dto.getName());
        role.setCompany(company);

        JobRole savedRole = jobRoleRepository.save(role);

        return mapToResponseDTO(savedRole);
    }

    //TODO: Criar métodos para editar e excluir cargos

    // Utilitário privado para manter o código limpo
    private JobRoleResponseDTO mapToResponseDTO(JobRole role) {
        JobRoleResponseDTO response = new JobRoleResponseDTO();
        response.setId(role.getId());
        response.setName(role.getName());
        response.setCompanyId(role.getCompany().getId());
        response.setCompanyName(role.getCompany().getName());
        return response;
    }
}