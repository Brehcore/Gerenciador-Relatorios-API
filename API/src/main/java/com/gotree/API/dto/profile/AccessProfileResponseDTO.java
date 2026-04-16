package com.gotree.API.dto.profile;

import com.gotree.API.enums.SystemPermission;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
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