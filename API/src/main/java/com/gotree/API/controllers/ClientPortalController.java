package com.gotree.API.controllers;

import com.gotree.API.config.security.ClientUserDetails;
import com.gotree.API.dto.auth.AuthenticationResponseDTO;
import com.gotree.API.dto.client.ClientFirstAccessRequestDTO;
import com.gotree.API.dto.client.ClientLoginDTO;
import com.gotree.API.dto.client.ClientSetupPasswordDTO;
import com.gotree.API.entities.AgendaEvent;
import com.gotree.API.exceptions.ResourceNotFoundException;
import com.gotree.API.repositories.ClientRepository;
import com.gotree.API.services.ClientPortalService;
import com.gotree.API.services.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST responsável por gerenciar o portal do cliente.
 * Este controlador fornece endpoints para:
 * - Primeiro acesso do cliente (solicitação de código e configuração de senha)
 * - Autenticação de clientes
 * - Consulta de agenda dos clientes autenticados
 */
@Tag(name = "Portal do Cliente", description = "Gerenciamento do portal do cliente.")
@RestController
@RequestMapping("/client-portal")
public class ClientPortalController {

    private final ClientPortalService clientPortalService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final ClientRepository clientRepository;

    public ClientPortalController(ClientPortalService clientPortalService, AuthenticationManager authenticationManager,
                                  JwtService jwtService, ClientRepository clientRepository) {
        this.clientPortalService = clientPortalService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.clientRepository = clientRepository;
    }

    /**
     * Solicita o primeiro acesso ao portal do cliente.
     * O cliente informa seu e-mail. Se o e-mail estiver cadastrado no sistema,
     * um código de verificação é gerado e enviado para o e-mail informado.
     * Este é o primeiro passo do fluxo de cadastro de senha para novos clientes.
     *
     * @param dto objeto contendo o e-mail do cliente para solicitação de acesso
     * @return ResponseEntity vazio com status 200 OK se a solicitação foi processada com sucesso
     */
    @Operation(summary = "Solicita o primeiro acesso ao portal do cliente", description = "O cliente informa seu e-mail cadastrado no sistema e recebe o código por e-mail")
    @PostMapping("/first-access/request")
    public ResponseEntity<Void> requestAccess(@RequestBody @Valid ClientFirstAccessRequestDTO dto) {
        clientPortalService.requestFirstAccess(dto);
        return ResponseEntity.ok().build();
    }

    /**
     * Passo 2: Configura a senha do cliente após verificação do código.
     * O cliente informa o código recebido por e-mail e define sua nova senha.
     * O sistema valida o código e, se correto, salva a senha criptografada no banco de dados,
     * permitindo que o cliente realize login posteriormente.
     *
     * @param dto objeto contendo o código de verificação e a nova senha do cliente
     * @return ResponseEntity vazio com status 200 OK se a senha foi configurada com sucesso
     */
    @Operation(summary = "Configura a senha do cliente", description = "O cliente recebe um código por e-mail e define sua nova senha.")
    @PostMapping("/first-access/setup")
    public ResponseEntity<Void> setupPassword(@RequestBody @Valid ClientSetupPasswordDTO dto) {
        clientPortalService.setupPassword(dto);
        return ResponseEntity.ok().build();
    }

    /**
     * Busca a agenda de eventos das empresas vinculadas ao cliente autenticado.
     * Este endpoint retorna todos os eventos de agenda relacionados às empresas
     * com as quais o cliente possui vínculo. É necessário que o cliente esteja
     * autenticado via token JWT válido.
     *
     * @param authentication objeto de autenticação contendo as credenciais do cliente logado
     * @return ResponseEntity contendo a lista de eventos da agenda do cliente
     */
    @Operation(summary = "Consulta agenda do cliente", description = "Retorna a agenda de eventos do cliente autenticado.")
    @GetMapping("/agenda")
    public ResponseEntity<List<AgendaEvent>> getMyAgenda(Authentication authentication) {
        // Assume que o authentication.getName() retorna o email do cliente (configurado no token JWT)
        String email = authentication.getName();
        List<AgendaEvent> events = clientPortalService.getClientAgenda(email);
        return ResponseEntity.ok(events);
    }

    /**
     * Realiza a autenticação do cliente no portal.
     * O cliente fornece e-mail e senha. O sistema valida as credenciais e,
     * se corretas, gera um token JWT que deve ser utilizado para acessar
     * os endpoints protegidos do portal.
     *
     * @param dto objeto contendo e-mail e senha do cliente
     * @return ResponseEntity contendo o token JWT de autenticação
     * @throws ResourceNotFoundException se o cliente não for encontrado após autenticação bem-sucedida
     */
    @Operation(summary = "Realiza login do cliente", description = "Autentica o cliente com e-mail e senha, retornando o token JWT.")
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponseDTO> login(@RequestBody @Valid ClientLoginDTO dto) {
        // 1. O AuthenticationManager usa o UserService.loadUserByUsername()
        // Como atualizamos o UserService, ele vai encontrar o cliente e validar a senha
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
        );

        // 2. Busca o cliente para passar para o gerador de token
        var client = clientRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado."));

        // 3. Gera o token
        // O ClientUserDetails implementa UserDetails, então o JwtService deve aceitá-lo
        var jwtToken = jwtService.generateToken(new ClientUserDetails(client));

        return ResponseEntity.ok(new AuthenticationResponseDTO(jwtToken));
    }
}