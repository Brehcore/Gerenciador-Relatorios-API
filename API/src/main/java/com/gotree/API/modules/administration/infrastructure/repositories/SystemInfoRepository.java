package com.gotree.API.modules.administration.infrastructure.repositories;

import com.gotree.API.modules.administration.domain.entities.SystemInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemInfoRepository extends JpaRepository<SystemInfo, Long> {

    // Como só teremos um registro, podemos fazer um metodo para pegar o primeiro
    @Query(value = "SELECT * FROM tb_system_info LIMIT 1", nativeQuery = true)
    SystemInfo findFirst();
}