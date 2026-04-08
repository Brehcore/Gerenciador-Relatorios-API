package com.gotree.API.config.security;

import com.gotree.API.entities.User;
import com.gotree.API.enums.SystemPermission;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record CustomUserDetails(User user) implements UserDetails {

	@Serial
	private static final long serialVersionUID = 1L;

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		List<GrantedAuthority> authorities = new ArrayList<>();

		// 1. Adiciona o Role base do utilizador (ex: "ROLE_ADMIN" ou "ROLE_USER")
		if (user.getRole() != null) {
			authorities.add(new SimpleGrantedAuthority(user.getRole().getRoleName()));
		}

		// 2. Adiciona as Permissões (Ações) vindas do Perfil de Acesso (AccessProfile)
		if (user.getProfile() != null && user.getProfile().getPermissions() != null) {
			for (SystemPermission permission : user.getProfile().getPermissions()) {
				// Adiciona o nome exato da permissão (ex: "VIEW_AGENDA", "CREATE_EVENT")
				authorities.add(new SimpleGrantedAuthority(permission.name()));
			}
		}

		return authorities;
	}

	@Override
	public String getPassword() {
		return user.getPassword();
	}

	@Override
	public String getUsername() {
		return user.getEmail();
	}
	@Override
	public boolean isEnabled() {
		return true; // Podes alterar futuramente se tiveres um campo "ativo" na base de dados
	}
}