package com.gotree.API.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO que representa um usuário que falhou na importação")
public class FailedUserDTO {

	@Schema(description = "E-mail do usuário", example = "falha@exemplo.com")
	private String email;
	@Schema(description = "Motivo da falha", example = "E-mail já cadastrado")
	private String reason;

	public FailedUserDTO(String email, String reason) {
		this.email = email;
		this.reason = reason;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

}
