package com.gotree.API.dto.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gotree.API.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO de resposta com os dados do usuário")
public class UserResponseDTO {

	@Schema(description = "ID único do usuário", example = "1")
	private Long id;

	@Schema(description = "Nome do usuário", example = "João da Silva")
	private String name;

	@Schema(description = "E-mail do usuário", example = "joao@exemplo.com")
	private String email;

	@Schema(description = "Telefone de contato", example = "11988887777")
	private String phone;

	@Schema(description = "Papel/Perfil do usuário no sistema", example = "USER")
	private UserRole role;

	@Schema(description = "ID do Perfil de Acesso", example = "1")
	private Long accessProfileId;

	@Schema(description = "Sigla do conselho de classe", example = "CREFITO")
	private String siglaConselhoClasse;

	@Schema(description = "Número do conselho de classe", example = "12345-F")
	private String conselhoClasse;

	@Schema(description = "Especialidade do profissional", example = "Fisioterapia do Trabalho")
	private String especialidade;

	@JsonFormat(pattern = "dd/MM/yyyy")
	@Schema(description = "Data de nascimento", example = "01/01/1990")
	private LocalDate birthDate;

	@JsonFormat(pattern = "dd/MM/yyyy")
	@Schema(description = "Data de validade do certificado digital", example = "31/12/2024")
	private LocalDate certificateValidity;

	@Schema(description = "Indica se o usuário possui certificado digital cadastrado")
	private boolean hasCertificate;

	@Schema(description = "CPF do usuário", example = "123.456.789-00")
	private String cpf;

}