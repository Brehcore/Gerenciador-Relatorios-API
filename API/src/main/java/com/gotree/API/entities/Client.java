package com.gotree.API.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_client")
@Data
@EqualsAndHashCode(of = "id")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(nullable = false)
    private String email; // O destinatário dos relatórios

    // Relacionamento: Um cliente tem várias empresas
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private List<Company> companies = new ArrayList<>();
}