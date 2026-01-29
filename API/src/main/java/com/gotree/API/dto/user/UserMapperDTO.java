package com.gotree.API.dto.user;

import com.gotree.API.entities.User;
import com.gotree.API.enums.UserRole;

import java.util.List;
import java.util.stream.Collectors;

public class UserMapperDTO {

	/**
	 * Converte o DTO de requisição em uma Entidade User.
	 */
	public static User toEntity(UserRequestDTO dto) {
		User user = new User();
		user.setName(dto.getName());
		user.setEmail(dto.getEmail());
		user.setPassword(dto.getPassword());
		user.setBirthDate(dto.getBirthDate());
		user.setPhone(dto.getPhone());
		user.setCpf(dto.getCpf());

		// Verifica se o DTO trouxe uma role, senão usa padrão
		if (dto.getRole() != null) {
			user.setRole(dto.getRole());
		} else {
			user.setRole(UserRole.USER);
		}

		user.setPasswordResetRequired(false);

		// Mapeando os novos campos
		user.setSiglaConselhoClasse(dto.getSiglaConselhoClasse());
		user.setConselhoClasse(dto.getConselhoClasse());
		user.setEspecialidade(dto.getEspecialidade());

		return user;
	}

	/**
	 * Converte uma Entidade User em um DTO de resposta.
	 */
	public static UserResponseDTO toDto(User user) {
		UserResponseDTO dto = new UserResponseDTO();
		dto.setId(user.getId());
		dto.setName(user.getName());
		dto.setEmail(user.getEmail());
		dto.setPhone(user.getPhone());

		dto.setRole(user.getRole());

		// Mapeando dados faltantes (CPF, Nascimento, Certificado)
		dto.setBirthDate(user.getBirthDate());
		dto.setCpf(user.getCpf());

		// Logica do Certificado Digital (Importante para o frontend)
		dto.setHasCertificate(user.getCertificatePath() != null && !user.getCertificatePath().isEmpty());
		dto.setCertificateValidity(user.getCertificateValidity());

		// Mapeando os campos do conselho
		dto.setSiglaConselhoClasse(user.getSiglaConselhoClasse());
		dto.setConselhoClasse(user.getConselhoClasse());
		dto.setEspecialidade(user.getEspecialidade());

		return dto;
	}

	/**
	 * Converte uma lista de Entidades User em uma lista de DTOs de resposta.
	 */
	public static List<UserResponseDTO> toDtoList(List<User> users) {
		return users.stream().map(UserMapperDTO::toDto).collect(Collectors.toList());
	}
}