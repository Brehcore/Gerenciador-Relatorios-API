package com.gotree.API.repositories;

import com.gotree.API.entities.OccupationalRiskReport;
import com.gotree.API.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OccupationalRiskReportRepository extends JpaRepository<OccupationalRiskReport, Long> {
    // Busca relatórios pelo técnico (para listar no dashboard)
    List<OccupationalRiskReport> findByTechnicianOrderByInspectionDateDesc(User technician);

    long countByTechnician(User technician);
}