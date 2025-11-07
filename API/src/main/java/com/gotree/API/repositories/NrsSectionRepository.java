package com.gotree.API.repositories;

import com.gotree.API.entities.NrsSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NrsSectionRepository extends JpaRepository<NrsSection, Long> {
}