package com.gotree.API.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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

    @JsonIgnore
    private String password; // Null até o primeiro acesso

    @JsonIgnore
    private String accessCode; //Null até o primeiro acesso

    @JsonIgnore
    private LocalDateTime accessCodeExpiration; // Validade do código

    // Relacionamento N:N (Um cliente vê várias empresas)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "tb_client_company",
            joinColumns = @JoinColumn(name = "client_id"),
            inverseJoinColumns = @JoinColumn(name = "company_id")
    )
    private Set<Company> companies = new HashSet<>();
}