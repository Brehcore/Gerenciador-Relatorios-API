package com.gotree.API.dto.profile;

import com.gotree.API.enums.SystemPermission;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
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
