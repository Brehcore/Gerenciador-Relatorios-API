package com.gotree.API.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponseDTO {

    private String token;
    private String type = "Bearer";

    public AuthenticationResponseDTO(String token) {
        this.token = token;
    }
}
