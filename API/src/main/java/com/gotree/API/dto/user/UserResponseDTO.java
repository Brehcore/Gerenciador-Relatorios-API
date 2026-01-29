package com.gotree.API.dto.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gotree.API.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {

	private Long id;
	private String name;
	private String email;
	private String phone;
	private UserRole role;
	private String siglaConselhoClasse;
	private String conselhoClasse;
	private String especialidade;

	@JsonFormat(pattern = "dd/MM/yyyy")
	private LocalDate birthDate;

	@JsonFormat(pattern = "dd/MM/yyyy")
	private LocalDate certificateValidity;

	private boolean hasCertificate;

	private String cpf;

}