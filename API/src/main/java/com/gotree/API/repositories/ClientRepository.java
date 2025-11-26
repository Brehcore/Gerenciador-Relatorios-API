package com.gotree.API.repositories;

import com.gotree.API.entities.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    // Metodo útil para validar se já existe um cliente com esse e-mail antes de salvar
    boolean existsByEmail(String email);

    // Metodo útil para buscar por e-mail (caso precise no futuro)
    Optional<Client> findByEmail(String email);
}