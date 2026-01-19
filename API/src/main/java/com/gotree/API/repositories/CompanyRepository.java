package com.gotree.API.repositories;

import com.gotree.API.entities.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByCnpj(String cnpj);

    boolean existsByClientsId(Long clientId);
}
