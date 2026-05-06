package com.gotree.API.modules.iam.presentation.dto;

import com.gotree.API.modules.iam.domain.enums.SystemPermission;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Set;

@Data
@Schema(description = "DTO relacionado a resposta dos perfis de acesso")
public class AccessProfileResponseDTO {

    @Schema(description = "Identificador do perfil", example = "1")
    private Long id;

    @Schema(description = "Nome do perfil", example = "Administrador")
    private String name;

    @Schema(description = "Permissão", example = "VIEW_AGENDA, EDIT_AGENDA")
    private Set<SystemPermission> permissions;
}