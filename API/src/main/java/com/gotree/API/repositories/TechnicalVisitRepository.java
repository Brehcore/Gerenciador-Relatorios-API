package com.gotree.API.repositories;

import com.gotree.API.entities.TechnicalVisit;
import com.gotree.API.entities.User;
import lombok.NonNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Interface de repositório para gerenciamento de Visitas Técnicas no sistema.
 * Esta interface estende JpaRepository para fornecer operações básicas de CRUD e
 * adiciona métodos personalizados para consultas específicas relacionadas às visitas técnicas.
 * Inclui funcionalidades para:
 * - Busca e filtragem de visitas por diferentes critérios
 * - Cálculos de duração e estatísticas
 * - Geração de relatórios e KPIs
 */
public interface TechnicalVisitRepository extends JpaRepository<TechnicalVisit, Long> {

    /**
     * Busca todas as visitas de um técnico, ordenadas por data decrescente.
     */
    @EntityGraph(attributePaths = {"clientCompany", "technician"})
    List<TechnicalVisit> findByTechnicianOrderByVisitDateDesc(User technician);

    /**
     * Recupera todas as visitas técnicas realizadas por um técnico específico,
     * incluindo os dados completos da empresa cliente por JOIN FETCH.
     * @param technician Usuário técnico para filtrar as visitas
     * @return Lista de visitas ordenada por data decrescente
     */
    @Query("SELECT v FROM TechnicalVisit v LEFT JOIN FETCH v.clientCompany WHERE v.technician = :technician ORDER BY v.visitDate DESC")
    List<TechnicalVisit> findAllWithCompanyByTechnician(@Param("technician") User technician);

    /**
     * Conta o número total de visitas realizadas por um técnico específico.
     * Utilizado para cálculo de KPIs e relatórios de produtividade.
     *
     * @param technician Técnico para contabilizar as visitas
     * @return Número total de visitas
     */
    long countByTechnician(User technician);

    /**
     * Conta o número de visitas realizadas por um técnico em uma empresa específica.
     *
     * @param technician Técnico para contabilizar as visitas
     * @param companyId  ID da empresa cliente
     * @return Número de visitas na empresa
     */
    long countByTechnicianAndClientCompanyId(User technician, Long companyId);

    // Cálculos de Duração
    @Query(value = "SELECT COALESCE(SUM(EXTRACT(EPOCH FROM (t.end_time - t.start_time))), 0) " +
            "FROM tb_technical_visit t WHERE t.technician_id = :userId AND t.end_time IS NOT NULL",
            nativeQuery = true)
    long findTotalVisitDurationInSeconds(@Param("userId") Long userId);

    @Query(value = "SELECT COALESCE(SUM(EXTRACT(EPOCH FROM (t.end_time - t.start_time))), 0) " +
            "FROM tb_technical_visit t WHERE t.end_time IS NOT NULL",
            nativeQuery = true)
    long findTotalVisitDurationInSeconds();

    /**
     * Verifica se existem visitas registradas para uma determinada empresa cliente.
     *
     * @param companyId ID da empresa para verificação
     * @return true se existirem visitas, false caso contrário
     */
    boolean existsByClientCompany_Id(Long companyId);

    /**
     * Verifica se existem visitas registradas para um determinado técnico.
     *
     * @param technicianId ID do técnico para verificação
     * @return true se existirem visitas, false caso contrário
     */
    boolean existsByTechnician_Id(Long technicianId);

    /**
     * Verifica se existem visitas registradas para um determinado setor.
     *
     * @param sectorId ID do setor para verificação
     * @return true se existirem visitas, false caso contrário
     */
    boolean existsBySector_Id(Long sectorId);

    @NonNull //Garante que o retorno (Optional) nunca será nulo
    @EntityGraph(attributePaths = {"clientCompany", "clientCompany.clients"})
    Optional<TechnicalVisit> findById(@NonNull Long id); //Garante que o parâmetro 'id' não pode ser nulo
}