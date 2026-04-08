package com.gotree.API.controllers;

import com.gotree.API.dto.client.ClientDTO;
import com.gotree.API.exceptions.ResourceNotFoundException;
import com.gotree.API.services.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador responsável por gerenciar operações relacionadas a clientes.
 * Fornece endpoints para buscar, listar, criar, atualizar e deletar recursos de clientes.
 * Aplica restrições de segurança utilizando anotações de autorização.
 */
@Tag(name = "Clientes", description = "Gerenciamento de clientes")
@RestController
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    /**
     * Busca um cliente específico pelo ID
     *
     * @param id ID do cliente a ser buscado
     * @return ResponseEntity contendo o ClientDTO se encontrado
     */
    @Operation(summary = "Busca um cliente pelo ID", description = "Retorna os detalhes de um cliente específico baseado no seu identificador único.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('') or hasRole('ADMIN')")
    public ResponseEntity<ClientDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.findById(id));
    }

    /**
     * Retrieves a paginated list of clients.
     *
     * @param pageable the pagination and sorting information
     * @return ResponseEntity containing a page of ClientDTO objects
     */
    @Operation(summary = "Lista todos os clientes paginados", description = "Recupera uma lista paginada de todos os clientes cadastrados no sistema.")
    @GetMapping
    @PreAuthorize("hasAuthority('') or hasRole('ADMIN')")
    public ResponseEntity<Page<ClientDTO>> getAll(
            @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<ClientDTO> clients = clientService.findAllPaginated(pageable);

        return ResponseEntity.ok(clients);
    }

    /**
     * Creates a new client and saves it to the database.
     *
     * @param dto the data transfer object containing the client information to be created
     * @return ResponseEntity containing the saved ClientDTO
     */
    @Operation(summary = "Cria um novo cliente", description = "Cria um novo cliente no sistema e persiste as informações no banco de dados.")
    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<ClientDTO> create(@RequestBody ClientDTO dto) {
        return ResponseEntity.ok(clientService.save(dto));
    }

    /**
     * Atualiza as informações de um cliente existente.
     *
     * @param id ID do cliente a ser atualizado
     * @param dto Dados do cliente a serem atualizados
     * @return ResponseEntity contendo o ClientDTO atualizado
     */
    @Operation(summary = "Atualiza um cliente existente", description = "Atualiza as informações de um cliente cadastrado com base no seu ID.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDIT_CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<ClientDTO> update(@PathVariable Long id, @RequestBody ClientDTO dto) {
        dto.setId(id);
        return ResponseEntity.ok(clientService.save(dto));
    }

    /**
     * Exclui um cliente pelo ID.
     * Apenas usuários com a role de ADMIN têm permissão para realizar esta operação.
     *
     * @param id ID do cliente a ser excluído
     * @return ResponseEntity contendo:
     *         - status 204 (No Content) se a exclusão for bem-sucedida
     *         - status 404 (Not Found) se o cliente não for encontrado
     *         - status 409 (Conflict) em caso de violação de integridade referencial
     */
    @Operation(summary = "Exclui um cliente", description = "Remove permanentemente um cliente do sistema pelo seu ID. Requer permissão de ADMIN.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_AGENDA') or hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            clientService.delete(id);
            return ResponseEntity.noContent().build();

        } catch (ResourceNotFoundException e) {
            // Retorna 404 se o cliente não existir
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Cliente não encontrado", "message", e.getMessage()));

        } catch (DataIntegrityViolationException e) {
            // Retorna 409 Conflict se tentar apagar cliente com empresas
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Violação de Integridade", "message", e.getMessage()));
        }
    }
}