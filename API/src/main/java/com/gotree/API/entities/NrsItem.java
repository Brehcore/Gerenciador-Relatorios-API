package com.gotree.API.entities;

import com.gotree.API.entities.enums.NrsCheckStatus; // <-- Importa o Enum
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "tb_nrs_item") // <-- Nova tabela
@Data
public class NrsItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 1024)
    private String description; // Ex: "Superfícies de trabalho limpas..."

    @Enumerated(EnumType.STRING)
    private NrsCheckStatus status; // Armazena CONFORME, NAO_CONFORME, etc.

    @Column(length = 512)
    private String justification; // Para a "Justificativa" do NA

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nrs_section_id") // <-- Link para a nova seção pai
    private NrsSection section;
}