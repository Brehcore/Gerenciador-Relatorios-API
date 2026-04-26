package com.gotree.API.dto.user;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.gotree.API.config.UserRoleDeserializer;
import com.gotree.API.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "DTO para atualização dos dados do usuário")
public class UserUpdateDTO {

	@Size(min = 8, max = 50, message = "Seu nome deve ter entre 3 e 50 caracteres")
	@Schema(description = "Nome do usuário", example = "João da Silva")
	private String name;

	@Size(min = 8, max = 20, message = "Telefone deve ter entre 8 e 20 caracteres")
	@Schema(description = "Telefone de contato", example = "11988887777")
	private String phone;

	@Schema(description = "CPF do usuário", example = "123.456.789-00")
	private String cpf;

	@JsonDeserialize(using = UserRoleDeserializer.class)
	@Schema(description = "Papel/Perfil do usuário no sistema", example = "USER")
	private UserRole role;

	@Schema(description = "Sigla do conselho de classe", example = "CREFITO")
	private String siglaConselhoClasse;

	@Schema(description = "Número do conselho de classe", example = "12345-F")
	private String conselhoClasse;

	@Schema(description = "Especialidade do profissional", example = "Fisioterapia do Trabalho")
	private String especialidade;

	@Schema(description = "E-mail do usuário", example = "joao@exemplo.com")
	private String email;

	@Schema(description = "ID do Perfil de Acesso do usuário", example = "1")
	private Long accessProfileId;
}
