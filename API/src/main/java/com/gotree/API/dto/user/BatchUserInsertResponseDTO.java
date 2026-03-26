package com.gotree.API.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "DTO de resposta para inserção em lote de usuários")
public class BatchUserInsertResponseDTO {

	@Schema(description = "Lista de usuários inseridos com sucesso")
	private List<UserResponseDTO> successUsers;

	@Schema(description = "Lista de usuários que falharam na inserção")
	private List<FailedUserDTO> failedUsers;

	public BatchUserInsertResponseDTO(List<UserResponseDTO> successUsers, List<FailedUserDTO> failedUsers) {
		super();
		this.successUsers = successUsers;
		this.failedUsers = failedUsers;
	}

	public List<UserResponseDTO> getSuccessUsers() {
		return successUsers;
	}

	public void setSuccessUsers(List<UserResponseDTO> successUsers) {
		this.successUsers = successUsers;
	}

	public List<FailedUserDTO> getFailedUsers() {
		return failedUsers;
	}

	public void setFailedUsers(List<FailedUserDTO> failedUsers) {
		this.failedUsers = failedUsers;
	}

}
