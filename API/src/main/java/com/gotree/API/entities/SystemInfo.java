package com.gotree.API.entities;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Entidade que armazena informações do sistema e da empresa consultora.
 * Esta classe mantém dados básicos como nome da empresa, CNPJ e logotipo
 * que são utilizados em relatórios e documentos gerados pelo sistema.
 *
 * @see OccupationalRiskReport
 */
@Entity
@Table(name = "tb_system_info")
@Data
public class SystemInfo {


    /**
     * Identificador único do registro de informações do sistema.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nome da empresa consultora (ex: "Go-Tree Consultoria LTDA").
     */
    private String companyName;

    /**
     * CNPJ da empresa consultora no formato XX.XXX.XXX/XXXX-XX.
     */
    private String cnpj;

    /**
     * Logotipo da empresa em formato Base64.
     * A imagem é convertida em texto para armazenamento no banco de dados.
     */
    @Column(columnDefinition = "TEXT")
    private String logoBase64;
}