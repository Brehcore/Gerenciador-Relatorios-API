package com.gotree.API.modules.iam.presentation.dto;

import com.gotree.API.modules.iam.domain.enums.SystemPermission;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
@Schema(description = "DTO para criação dos perfis do acesso")
public class AccessProfileRequestDTO {

    @NotBlank
    @Schema(description = "Nome do perfil", example = "Agenda")
    private String name;

    @NotEmpty
    @Schema(description = "Permissão", example = "VIEW_AGENDA")
    private Set<SystemPermission> permissions;
}
