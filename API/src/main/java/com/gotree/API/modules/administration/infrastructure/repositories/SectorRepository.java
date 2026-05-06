package com.gotree.API.modules.administration.infrastructure.repositories;

import com.gotree.API.modules.administration.domain.entities.Sector;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectorRepository extends JpaRepository<Sector, Long> {
}
