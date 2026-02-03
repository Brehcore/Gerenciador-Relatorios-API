package com.gotree.API.repositories;

import com.gotree.API.entities.AgendaEvent;
import com.gotree.API.entities.User;
import com.gotree.API.enums.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AgendaEventRepository extends JpaRepository<AgendaEvent, Long> {

    @Query("SELECT e FROM AgendaEvent e JOIN FETCH e.user ORDER BY e.eventDate ASC")
    List<AgendaEvent> findAllWithUserByOrderByEventDateAsc();

    List<AgendaEvent> findAllByOrderByEventDateAsc();

    // Busca pelo relacionamento com TechnicalVisit
    Optional<AgendaEvent> findByTechnicalVisit_Id(Long technicalVisitId);

    List<AgendaEvent> findByUserAndEventTypeIn(User user, List<String> types);

    // --- QUERIES DE VALIDAÇÃO DE CONFLITO ---

    // 1. Conta eventos (existente - usado para verificar disponibilidade geral)
    long countByUserAndEventDate(User user, LocalDate date);

    // 2. Conta eventos por turno (existente)
    long countByUserAndEventDateAndShift(User user, LocalDate date, Shift shift);

    // 3. Validação de Bloqueio (Usado no validateReportSubmission)
    List<AgendaEvent> findByUserAndEventDateAndShift(User user, LocalDate eventDate, Shift shift);

    // 4. CORREÇÃO DO ERRO: Busca eventos por intervalo (Usado para pintar o calendário)
    List<AgendaEvent> findByUserAndEventDateBetween(User user, LocalDate startDate, LocalDate endDate);

    @Query("SELECT e FROM AgendaEvent e JOIN FETCH e.user WHERE e.user.id = :userId ORDER BY e.eventDate ASC")
    List<AgendaEvent> findByUserIdWithUserOrderByEventDateAsc(@Param("userId") Long userId);

    /**
     * Busca eventos onde:
     * 1. A visita técnica pertence a uma das empresas listadas (Eventos automáticos)
     * OR
     * 2. O evento foi vinculado diretamente a uma das empresas listadas (Eventos manuais)
     */
    @Query("SELECT ae FROM AgendaEvent ae " +
            "LEFT JOIN ae.technicalVisit tv " +
            "LEFT JOIN tv.clientCompany cc " +
            "WHERE (cc.id IN :companyIds) OR (ae.company.id IN :companyIds) " +
            "ORDER BY ae.eventDate DESC")
    List<AgendaEvent> findByClientCompanyIds(@Param("companyIds") List<Long> companyIds);

    // --- AGENDA GLOBAL (Busca eventos manuais de TODOS) ---
    @Query("SELECT e FROM AgendaEvent e JOIN FETCH e.user WHERE e.eventDate BETWEEN :startDate AND :endDate")
    List<AgendaEvent> findAllByEventDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // --- CONCORRÊNCIA (Busca eventos de TODOS em data/turno específicos) ---
    @Query("SELECT e FROM AgendaEvent e JOIN FETCH e.user WHERE e.eventDate = :date AND e.shift = :shift")
    List<AgendaEvent> findAllByEventDateAndShift(@Param("date") LocalDate date, @Param("shift") Shift shift);
}