package com.gotree.API.entities;

import com.gotree.API.config.UserRoleConverter;
import com.gotree.API.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "tb_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class User implements Serializable, UserDetails {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String password;
    private LocalDate birthDate;
    private String phone;
    private String cpf;

    @Column(nullable = false)
    @Convert(converter = UserRoleConverter.class)
    private UserRole role = UserRole.USER;

    @Column(name = "password_reset_required")
    private Boolean passwordResetRequired = false;

    private String siglaConselhoClasse;
    private String conselhoClasse;
    private String especialidade;

    // Assinatura Digital
    @Column(name = "certificate_path")
    private String certificatePath;

    @Column(name = "certificate_password")
    private String certificatePassword;

    @Column(name = "certificate_validity")
    private LocalDate certificateValidity;


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Pega o nome da role (ex: "ADMIN" ou "ROLE_ADMIN")
        String roleName = role.getRoleName();

        // Garante que tenha o prefixo ROLE_ para o Spring Security reconhecer
        if (!roleName.startsWith("ROLE_")) {
            roleName = "ROLE_" + roleName;
        }

        return List.of(new SimpleGrantedAuthority(roleName));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // Método com lógica customizada (não é um getter simples, por isso é mantido)
    public int getAge() {
        return Period.between(this.birthDate, LocalDate.now()).getYears();
    }
}