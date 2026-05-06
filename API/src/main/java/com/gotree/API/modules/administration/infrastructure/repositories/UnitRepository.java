package com.gotree.API.modules.administration.infrastructure.repositories;


import com.gotree.API.modules.administration.domain.entities.Unit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnitRepository extends JpaRepository<Unit, Long> {
}
