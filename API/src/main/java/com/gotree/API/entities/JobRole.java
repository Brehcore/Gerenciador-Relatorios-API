package com.gotree.API.entities;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Entidade que representa um cargo ou função dentro de uma empresa.
 * Esta classe armazena informações sobre cargos específicos e sua relação
 * com a empresa à qual pertencem.
 */
@Entity
@Table(name = "tb_job_role")
@Data
public class JobRole {


    /**
     * Identificador único do cargo.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nome do cargo ou função.
     * Este campo não pode ser nulo.
     */
    @Column(nullable = false)
    private String name;

    /**
     * Empresa à qual este cargo está vinculado.
     * Relacionamento muitos-para-um com carregamento lazy.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;
}