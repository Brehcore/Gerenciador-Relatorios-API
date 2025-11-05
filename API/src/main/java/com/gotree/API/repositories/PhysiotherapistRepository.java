package com.gotree.API.repositories;

import com.gotree.API.entities.Physiotherapist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhysiotherapistRepository extends JpaRepository<Physiotherapist, Long> {
}