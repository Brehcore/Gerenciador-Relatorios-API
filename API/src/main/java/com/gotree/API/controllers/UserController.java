package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.user.CertificateUploadDTO;
import com.gotree.API.dto.user.ChangeEmailRequestDTO;
import com.gotree.API.dto.user.ChangePasswordRequestDTO;
import com.gotree.API.dto.user.CompletePasswordResetDTO;
import com.gotree.API.dto.user.UserRequestDTO;
import com.gotree.API.dto.user.UserResponseDTO;
import com.gotree.API.dto.user.UserUpdateDTO;
import com.gotree.API.entities.User;
import com.gotree.API.exceptions.ResourceNotFoundException;
import com.gotree.API.mappers.UserMapper;
import com.gotree.API.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.util.Map;

/**
 * Controlador responsável por gerenciar as operações relacionadas aos usuários.
 * Fornece endpoints para criação, atualização, busca e exclusão de usuários,
 * além de funcionalidades de gerenciamento de perfil como alteração de senha,
 * e-mail e certificados digitais.
 */
@Tag(name = "Gerenciamento de Usuários", description = "Gerenciar as operações relacionadas aos usuários")
@RestController
@RequestMapping(value = "/users")
public class UserController {

	private final UserService userService;
	private final UserMapper userMapper;
	private static final Logger log = LoggerFactory.getLogger(UserController.class);
	
	public UserController(UserService userService, UserMapper userMapper) {
		this.userService = userService;
		this.userMapper = userMapper;
	}

	/**
	 * Busca todos os usuários de forma paginada.
	 * Apenas administradores têm acesso a este endpoint.
	 *
	 * @param pageable Informações de paginação e ordenação
	 * @return Página contendo os usuários encontrados
	 */
	@Operation(summary = "Busca de todos os usuários", description = "Realiza a busca de todos os usuários de forma paginada.")
	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Page<UserResponseDTO>> findAll(
			@PageableDefault(size = 5, sort = "id") Pageable pageable
	) {
		Page<User> usersPage = userService.findAll(pageable);

		Page<UserResponseDTO> dtoPage = usersPage.map(userMapper::toDto);

		return ResponseEntity.ok(dtoPage);
	}

	/**
	 * Busca um usuário específico pelo seu ID.
	 * Apenas administradores têm acesso a este endpoint.
	 *
	 * @param id Identificador único do usuário
	 * @return Dados do usuário encontrado
	 */
	@Operation(summary = "Busca de usuários por ID", description = "Realiza a busca de um usuário específico pelo ID.")
	@GetMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UserResponseDTO> findById(@PathVariable Long id) {
		User user = userService.findById(id);
		return ResponseEntity.ok(userMapper.toDto(user));
	}

	/**
	 * Cria um novo usuário no sistema.
	 * Apenas administradores têm acesso a este endpoint.
	 *
	 * @param dto Dados do usuário a ser criado
	 * @return Dados do usuário criado
	 */
	@Operation(summary = "Criação de usuário", description = "Cria um novo usuário no sistema.")
	@PostMapping("/insert")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UserResponseDTO> insertUser(@RequestBody @Valid UserRequestDTO dto) {
		User createdUser = userService.insertUser(dto);
		return ResponseEntity.ok(userMapper.toDto(createdUser));
	}

	/**
	 * Atualiza os dados de um usuário existente.
	 * Apenas administradores têm acesso a este endpoint.
	 *
	 * @param id  Identificador único do usuário
	 * @param dto Novos dados do usuário
	 * @return Dados atualizados do usuário
	 */
	@Operation(summary = "Atualização de usuário", description = "Atualiza os dados de um usuário existente no sistema.")
	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long id, @RequestBody @Valid UserUpdateDTO dto) {
		User updateUser = userService.updateUser(id, dto);
		return ResponseEntity.ok(userMapper.toDto(updateUser));
	}

	/**
	 * Redefine a senha de um usuário para o valor padrão.
	 * Marca o campo passwordResetRequired como verdadeiro, forçando o usuário
	 * a alterar a senha no próximo login.
	 * Apenas administradores têm acesso a este endpoint.
	 *
	 * @param id Identificador único do usuário
	 * @return Mensagem de confirmação da operação
	 */
	@Operation(summary = "Redefinição de senha", description = "Redefine a senha de um usuário para o valor padrão e força a alteração no próximo login.")
	@PutMapping("/admin/reset-password/{id}")
	@PreAuthorize("hasRole('ADMIN')") // garante que só admin pode chamar
	public ResponseEntity<?> resetPassword(@PathVariable Long id) {
		userService.resetPassword(id);
		return ResponseEntity
				.ok(Map.of("message", "Senha redefinida com sucesso e campo passwordResetRequired Ativado"));

	}

	/**
	 * Permite que o usuário defina uma nova senha após o administrador resetar sua conta.
	 * Só funciona se a flag passwordResetRequired estiver ativada.
	 *
	 * @param authentication Informações de autenticação do usuário
	 * @param dto            Dados contendo apenas a nova senha
	 * @return Mensagem de confirmação da operação
	 */
	@Operation(
			summary = "Completar redefinição de senha forçada",
			description = "Define uma nova senha para o usuário logado caso o administrador tenha forçado um reset (passwordResetRequired = true)."
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Senha atualizada com sucesso. Flag de reset desativada."),
			@ApiResponse(responseCode = "400", description = "Dados inválidos fornecidos ou usuário não possui reset pendente.")
	})
	@PutMapping("/me/complete-password-reset")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> completePasswordReset(Authentication authentication,
	                                               @Valid @RequestBody CompletePasswordResetDTO dto) {
		String userEmail = authentication.getName();

		userService.completePasswordReset(userEmail, dto.getNewPassword());

		return ResponseEntity.ok(Map.of("message", "Senha atualizada com sucesso. Bem-vindo(a) de volta!"));
	}

	/**
	 * Remove um usuário do sistema.
	 * Apenas administradores têm acesso a este endpoint.
	 *
	 * @param id Identificador único do usuário
	 * @return Resposta vazia em caso de sucesso ou mensagem de erro
	 */
	@Operation(summary = "Exclusão de usuário", description = "Remove um usuário do sistema.")
	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> deleteUser(@PathVariable Long id) {
		try {
			userService.deleteUser(id);
			return ResponseEntity.noContent().build();

		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));

		} catch (ResourceNotFoundException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
		}
	}

	/**
	 * Retorna os dados do usuário atualmente autenticado.
	 *
	 * @param authentication Informações de autenticação do usuário
	 * @return Dados do usuário logado
	 */
	@Operation(summary = "Busca de usuário atual", description = "Retorna os dados do usuário atualmente autenticado.")
	@GetMapping("/me")
	public ResponseEntity<UserResponseDTO> getCurrentUser(Authentication authentication) {

		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

		User loggedInUser = userDetails.user();

		UserResponseDTO userDto = userMapper.toDto(loggedInUser);

		return ResponseEntity.ok(userDto);
	}

	/**
	 * Permite que o usuário autenticado altere sua própria senha.
	 * Requer a senha atual para validação.
	 *
	 * @param authentication Informações de autenticação do usuário
	 * @param dto            Dados contendo a senha atual e a nova senha
	 * @return Mensagem de confirmação da operação
	 */
	@Operation(summary = "Alteração de senha", description = "Permite que o usuário autenticado altere sua própria senha.")
	@PutMapping("/me/change-password")
	@PreAuthorize("isAuthenticated()") // Garante que o utilizador esteja logado
	public ResponseEntity<?> changePassword(Authentication authentication,
											@Valid @RequestBody ChangePasswordRequestDTO dto) {
		String userEmail = authentication.getName();

		userService.changePassword(userEmail, dto.getNewPassword(), dto.getCurrentPassword());

		return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso."));
	}

	/**
	 * Permite que o usuário autenticado altere seu endereço de e-mail.
	 * Requer a senha atual para validação.
	 *
	 * @param authentication Informações de autenticação do usuário
	 * @param dto            Dados contendo o novo e-mail e a senha atual
	 * @return Mensagem de confirmação da operação
	 */
	@Operation(summary = "Alteração de e-mail", description = "Permite que o usuário autenticado altere seu endereço de e-mail.")
	@PutMapping("/me/change-email")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Map<String, String>> changeEmail(Authentication authentication,
										@Valid @RequestBody ChangeEmailRequestDTO dto) {
		String currentUserEmail = authentication.getName();

		userService.changeEmail(currentUserEmail, dto.getNewEmail(), dto.getCurrentPassword());

		return ResponseEntity.ok(Map.of("message", "E-mail alterado com sucesso."));
	}

	/**
	 * Permite que o usuário autenticado faça upload de um certificado digital.
	 * O certificado é validado e armazenado para uso em assinaturas digitais.
	 *
	 * @param authentication Informações de autenticação do usuário
	 * @param dto            Dados do certificado incluindo o arquivo e a senha
	 * @return Mensagem de confirmação ou erro da operação
	 */
	@Operation(summary = "Upload de certificado digital", description = "Permite que o usuário autenticado faça upload de um certificado digital.")
	@PostMapping(value = "/me/certificate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Map<String, String>> uploadCertificate(
			Authentication authentication,
			@ModelAttribute @Valid CertificateUploadDTO dto) {

		String userEmail = authentication.getName();

		CertificateUploadDTO cleanDto = new CertificateUploadDTO();

		cleanDto.setPassword(dto.getPassword());

		cleanDto.setFile(dto.getFile());

		try {
			userService.uploadCertificate(userEmail, cleanDto);
			return ResponseEntity.ok(Map.of("message", "Certificado digital configurado com sucesso."));

		} catch (IllegalArgumentException e) {
			log.warn("Falha de validação no certificado do usuário {}: {}", userEmail, e.getMessage());

			String safeMessage = e.getMessage() != null ? HtmlUtils.htmlEscape(e.getMessage()) : "Erro na validação";
			return ResponseEntity.badRequest().body(Map.of("error", safeMessage));

		} catch (Exception e) {
			log.error("Erro interno ao processar certificado do usuário {}", userEmail, e);

			return ResponseEntity.internalServerError().body(Map.of(
					"error", "Ocorreu um erro interno ao processar o certificado. Nossa equipe já foi notificada."
			));
		}
	}

	/**
	 * Remove o certificado digital do usuário autenticado.
	 *
	 * @param authentication Informações de autenticação do usuário
	 * @return Mensagem de confirmação ou erro da operação
	 */
	@Operation(summary = "Remoção de certificado digital", description = "Remove o certificado digital do usuário autenticado.")
	@DeleteMapping("/me/certificate")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> removeCertificate(Authentication authentication) {
		String userEmail = authentication.getName();
		try {
			userService.removeCertificate(userEmail);
			return ResponseEntity.ok(Map.of("message", "Certificado removido com sucesso."));
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body(Map.of("error", "Erro ao remover certificado: " + e.getMessage()));
		}
	}
}
