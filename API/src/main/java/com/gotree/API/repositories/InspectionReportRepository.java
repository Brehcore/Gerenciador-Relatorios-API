package com.gotree.API.repositories;

import com.gotree.API.entities.InspectionReport;
import com.gotree.API.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
// Em InspectionReportRepository.java
public interface InspectionReportRepository extends JpaRepository<InspectionReport, Long> {

    List<InspectionReport> findByTechnician(User technician);

    List<InspectionReport> findTop5ByTechnicianOrderByInspectionDateDesc(User technician);

    @Query("SELECT r FROM InspectionReport r JOIN FETCH r.company WHERE r.technician = :technician")
    List<InspectionReport> findAllWithCompanyByTechnician(@Param("technician") User technician);
}