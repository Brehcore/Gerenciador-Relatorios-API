package com.gotree.API.repositories;

import com.gotree.API.entities.TechnicalVisit;
import com.gotree.API.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TechnicalVisitRepository extends JpaRepository<TechnicalVisit, Long> {

    List<TechnicalVisit> findByTechnicianOrderByVisitDateDesc(User technician);

    @Query("SELECT v FROM TechnicalVisit v LEFT JOIN FETCH v.clientCompany WHERE v.technician = :technician ORDER BY v.visitDate DESC")
    List<TechnicalVisit> findAllWithCompanyByTechnician(@Param("technician") User technician);
}