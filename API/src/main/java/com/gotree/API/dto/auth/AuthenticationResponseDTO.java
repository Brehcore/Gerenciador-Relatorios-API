package com.gotree.API.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "DTO de resposta de autenticação contendo o token JWT")
public class AuthenticationResponseDTO {

    @Schema(description = "Token de acesso JWT")
    private String token;

    @Schema(description = "Tipo do token", example = "Bearer")
    private String type = "Bearer";

    private Long userId;

    public AuthenticationResponseDTO(String token, Long userId) {
        this.token = token;
        this.userId = userId;
    }
}
