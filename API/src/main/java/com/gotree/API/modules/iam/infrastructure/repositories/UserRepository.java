package com.gotree.API.modules.iam.infrastructure.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gotree.API.modules.iam.domain.entities.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

   boolean existsByProfileId(Long profileId);

}
