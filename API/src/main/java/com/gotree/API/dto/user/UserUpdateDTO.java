package com.gotree.API.dto.user;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateDTO {

	@Size(min = 8, max = 50, message = "Seu nome deve ter entre 3 e 50 caracteres")
	private String name;
	@Size(min = 8, max = 20, message = "Telefone deve ter entre 8 e 20 caracteres")
	private String phone;

	private String cpf;
	private String siglaConselhoClasse;
	private String conselhoClasse;
	private String especialidade;
	private String email;
}
