package com.gotree.API.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_nrs_section") // <-- Nova tabela
@Data
public class NrsSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title; // Ex: "Higiene e Limpeza (NR 24...)"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id") // <-- Link para o InspectionReport principal
    private InspectionReport report;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NrsItem> items = new ArrayList<>(); // <-- Link para os novos NrsItem
}