package com.gotree.API.repositories;

import com.gotree.API.entities.NrsItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NrsItemRepository extends JpaRepository<NrsItem, Long> {
}