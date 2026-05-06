package com.gotree.API.modules.professionals.infrastructure.repositories;

import com.gotree.API.modules.professionals.domain.entities.Physiotherapist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhysiotherapistRepository extends JpaRepository<Physiotherapist, Long> {
}