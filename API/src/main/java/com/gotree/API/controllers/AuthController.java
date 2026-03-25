package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.LoginRequestDTO;
import com.gotree.API.entities.User;
import com.gotree.API.services.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controlador responsável por gerenciar as operações relacionadas à autenticação de usuários.
 * Disponibiliza endpoints para login e geração de tokens JWT.
 */
@Tag(name = "Autenticação", description = "Operações relacionadas à autenticação e geração de tokens JWT.")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
	
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	/**
	 * Autentica um usuário e gera um token JWT.
	 *
	 * @param request DTO contendo as credenciais do usuário (email e senha)
	 * @return ResponseEntity contendo o token JWT, flag de reset de senha e role do usuário
	 * @throws org.springframework.security.core.AuthenticationException se as credenciais forem inválidas
	 */
	@Operation(summary = "Realiza login", description = "Autentica o usuário com email e senha, retornando o token JWT e informações de perfil.")
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody @Valid LoginRequestDTO request) {

		// Cria o "pacote de login" com o email e senha fornecidos
		UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(request.getEmail(),
				request.getPassword());

		// Faz a autenticação de fato (verifica se existe, se a senha está certa, etc)
		Authentication authentication = authenticationManager.authenticate(authToken);

		// Após autenticar pega os dados do usuário
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		User user = userDetails.user();

		System.out.println(user.getPasswordResetRequired());

		// Gera o token jwt com base nesse usuário

		String jwt = jwtService.generateToken(userDetails);

		return ResponseEntity.ok(Map.of(
				"token", jwt,
				"passwordResetRequired", user.getPasswordResetRequired(),
				"role", user.getRole().getRoleName()
		));
	}

}