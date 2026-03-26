package com.gotree.API.dto.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.gotree.API.config.UserRoleDeserializer;
import com.gotree.API.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "DTO para requisição de criação de usuário")
public class UserRequestDTO {

	@NotBlank(message = "Seu Nome é obrigatório")
	@Schema(description = "Nome completo do usuário", example = "João da Silva")
	private String name;

	@NotBlank(message = "Seu email é obrigatório")
	@Schema(description = "E-mail do usuário", example = "joao@exemplo.com")
	private String email;

	@NotBlank(message = "Sua senha é obrigatório")
	@Size(min = 8, message = "Sua senha deve ter no mínimo 8 caracteres")
	@Schema(description = "Senha do usuário", example = "senha123")
	private String password;

	@NotNull(message = "Sua data de nascimento é obrigatória")
	@Past(message = "A data de nascimento deve ser no passado")
	@JsonFormat(pattern = "dd/MM/yyyy")
	@Schema(description = "Data de nascimento", example = "01/01/1990")
	private LocalDate birthDate;

	@Size(min = 8, max = 20, message = "Telefone deve ter entre 8 e 20 caracteres")
	@NotBlank(message = "Seu telefone é obrigatório")
	@Schema(description = "Telefone de contato", example = "11988887777")
	private String phone;

	@NotBlank
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

}