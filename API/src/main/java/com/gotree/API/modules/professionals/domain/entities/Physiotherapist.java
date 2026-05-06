package com.gotree.API.modules.professionals.domain.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "tb_physiotherapist")
@Data
public class Physiotherapist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true) // Garante que o CREFITO seja único
    private String crefito;
}