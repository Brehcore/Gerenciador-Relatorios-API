package com.gotree.API.repositories;

import com.gotree.API.entities.AccessProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessProfileRepository extends JpaRepository<AccessProfile, Long> {
}
