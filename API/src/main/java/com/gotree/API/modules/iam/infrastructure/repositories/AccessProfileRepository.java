package com.gotree.API.modules.iam.infrastructure.repositories;

import com.gotree.API.modules.iam.domain.entities.AccessProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessProfileRepository extends JpaRepository<AccessProfile, Long> {

    boolean existsByName(String name);
}
