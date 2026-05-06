package com.gotree.API.modules.administration.infrastructure.repositories;

import com.gotree.API.modules.administration.domain.entities.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByCnpj(String cnpj);

    boolean existsByClientsId(Long clientId);
}
