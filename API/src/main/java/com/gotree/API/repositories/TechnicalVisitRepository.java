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

    /**
     * Encontra todas as visitas pelo usuário autenticado que têm uma "próxima visita" agendada,
     * já trazendo a Empresa (clientCompany), a Unidade (unit) e o Setor (sector).
     */
    @Query("SELECT v FROM TechnicalVisit v " +
            "LEFT JOIN FETCH v.clientCompany " +
            "LEFT JOIN FETCH v.unit " +
            "LEFT JOIN FETCH v.sector " +
            "WHERE v.technician = :technician AND v.nextVisitDate IS NOT NULL " +
            "ORDER BY v.nextVisitDate ASC")
    List<TechnicalVisit> findAllScheduledWithCompanyByTechnician(@Param("technician") User technician);

    /**
     * Encontra TODAS as visitas de TODOS os usuários que têm
     * uma "próxima visita" agendada.
     */
    @Query("SELECT v FROM TechnicalVisit v " +
            "LEFT JOIN FETCH v.clientCompany " +
            "LEFT JOIN FETCH v.unit " +
            "LEFT JOIN FETCH v.sector " +
            "WHERE v.nextVisitDate IS NOT NULL " +
            "ORDER BY v.nextVisitDate ASC")
    List<TechnicalVisit> findAllScheduledWithCompany();
}