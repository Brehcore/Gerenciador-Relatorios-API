package com.gotree.API.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "DTO para requisição de login")
public class LoginRequestDTO {

    @NotBlank
    @Email
    @Schema(description = "E-mail do usuário", example = "usuario@exemplo.com")
    private String email;

    @NotBlank
    @Schema(description = "Senha do usuário", example = "senha123")
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
