package com.gotree.API.entities;

import com.gotree.API.enums.SystemPermission;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.Set;

@Entity
@Table(name = "access_profiles")
@Data
public class AccessProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name; // Ex: "Segurança", "Comercial"

    // Você pode armazenar as permissões como uma lista de Strings
    // Ex: ["VIEW_AGENDA", "CREATE_DOCS", "VIEW_REPORTS"]
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_permissions", joinColumns = @JoinColumn(name = "profile_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission")
    private Set<SystemPermission> permissions;
}
