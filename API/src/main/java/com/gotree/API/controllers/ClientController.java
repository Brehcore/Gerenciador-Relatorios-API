package com.gotree.API.controllers;

import com.gotree.API.dto.client.ClientDTO;
import com.gotree.API.dto.company.CompanyResponseDTO;
import com.gotree.API.exceptions.ResourceNotFoundException;
import com.gotree.API.services.ClientService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller responsável pelo gerenciamento de Clientes.
 * Fornece endpoints para operações CRUD (Criar, Ler, Atualizar, Deletar) de clientes.
 * Implementa paginação para listagem de clientes e tratamento de erros para deleção.
 * Todos os endpoints requerem autenticação. Algumas operações requerem papel ADMIN.
 */
@RestController
@RequestMapping("/clients")
@PreAuthorize("isAuthenticated()")
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
    @GetMapping("/{id}")
    public ResponseEntity<ClientDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.findById(id));
    }

    /**
     * Retorna a lista paginada de clientes cadastrados.
     *
     * @param pageable Objeto de paginação com parâmetros:
     *                 page (página atual, default 0)
     *                 size (itens por página, default 10)
     *                 sort (campo para ordenação, default "name")
     *                 direction (direção da ordenação, default ASC)
     * @return ResponseEntity com Page contendo os ClientDTOs
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ClientDTO>> getAll(
            @PageableDefault(page = 0, size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<ClientDTO> clients = clientService.findAllPaginated(pageable);

        return ResponseEntity.ok(clients);
    }

    /**
     * Cria um novo cliente
     *
     * @param dto Dados do cliente a ser criado
     * @return ResponseEntity contendo o ClientDTO criado
     */
    @PostMapping
    public ResponseEntity<ClientDTO> create(@RequestBody ClientDTO dto) {
        return ResponseEntity.ok(clientService.save(dto));
    }

    /**
     * Atualiza um cliente existente
     *
     * @param id  ID do cliente a ser atualizado
     * @param dto Novos dados do cliente
     * @return ResponseEntity contendo o ClientDTO atualizado
     */
    @PutMapping("/{id}")
    public ResponseEntity<ClientDTO> update(@PathVariable Long id, @RequestBody ClientDTO dto) {
        dto.setId(id);
        return ResponseEntity.ok(clientService.save(dto));
    }

    /**
     * Endpoint para deletar cliente com tratamento de erro.
     * Retorna 409 Conflict se houver empresas vinculadas.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
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