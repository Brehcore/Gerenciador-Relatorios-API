package com.gotree.API.controllers;

import com.gotree.API.dto.client.ClientDTO;
import com.gotree.API.services.ClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller responsável pelo gerenciamento de Clientes.
 * Todos os endpoints requerem autenticação com papel ADMIN.
 */
@RestController
@RequestMapping("/clients")
@PreAuthorize("hasRole('ADMIN')")
public class ClientController {
    
    

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    /**
     * Retorna a lista de todos os clientes cadastrados
     *
     * @return Lista de ClientDTO com todos os clientes
     */
    @GetMapping
    public List<ClientDTO> getAll() {
        return clientService.findAll();
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
     * Remove um cliente do sistema
     *
     * @param id ID do cliente a ser removido
     * @return ResponseEntity sem conteúdo e status 204
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}