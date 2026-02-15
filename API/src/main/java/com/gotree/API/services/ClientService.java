package com.gotree.API.services;

import com.gotree.API.dto.client.ClientDTO;

import com.gotree.API.entities.Client;
import com.gotree.API.entities.Company;
import com.gotree.API.exceptions.ResourceNotFoundException;
import com.gotree.API.mappers.ClientMapper;
import com.gotree.API.repositories.ClientRepository;
import com.gotree.API.repositories.CompanyRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

/**
 * Serviço responsável por gerenciar operações relacionadas a clientes.
 * Fornece métodos para busca, criação, atualização e deleção de clientes,
 * além de gerenciar seus relacionamentos com empresas.
 */
@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final CompanyRepository companyRepository;
    private final ClientMapper clientMapper;

    /**
     * Construtor do serviço de clientes.
     *
     * @param clientRepository  Repositório de clientes
     * @param companyRepository Repositório de empresas
     * @param clientMapper      Mapeador para conversão entre entidade e DTO
     */
    public ClientService(ClientRepository clientRepository,
                         CompanyRepository companyRepository,
                         ClientMapper clientMapper) {
        this.clientRepository = clientRepository;
        this.companyRepository = companyRepository;
        this.clientMapper = clientMapper;
    }

    /**
     * Busca todos os clientes cadastrados.
     *
     * @return Lista de DTOs contendo informações dos clientes
     */
    @Transactional(readOnly = true)
    public List<ClientDTO> findAll() {
        return clientMapper.toDtoList(clientRepository.findAll());
    }

    /**
     * Busca um cliente pelo seu ID.
     *
     * @param id ID do cliente a ser buscado
     * @return DTO com as informações do cliente
     * @throws RuntimeException quando o cliente não é encontrado
     */
    @Transactional(readOnly = true)
    public ClientDTO findById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        return clientMapper.toDto(client);
    }

    /**
     * Busca clientes de forma paginada.
     *
     * @param pageable Informações de paginação
     * @return Página contendo DTOs dos clientes
     */
    @Transactional(readOnly = true)
    public Page<ClientDTO> findAllPaginated(Pageable pageable) {
        Page<Client> clients = clientRepository.findAll(pageable);

        return clients.map(this::mapToResponseDto);
    }

    /**
     * Salva ou atualiza um cliente.
     * Se o ID estiver presente, atualiza o cliente existente.
     * Também gerencia os vínculos com empresas.
     *
     * @param dto DTO contendo dados do cliente
     * @return DTO do cliente salvo
     * @throws RuntimeException quando tenta atualizar um cliente inexistente
     */
    @Transactional
    public ClientDTO save(ClientDTO dto) {
        Client client = new Client();
        if (dto.getId() != null) {
            client = clientRepository.findById(dto.getId())
                    .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        }

        client.setName(dto.getName());
        client.setEmail(dto.getEmail());

        // Lógica de Vínculo N:N
        if (dto.getCompanyIds() != null) {
            // 1. Busca as empresas pelos IDs enviados
            List<Company> companiesToLink = companyRepository.findAllById(dto.getCompanyIds());

            // 2. Atualiza a coleção do cliente
            // Como é N:N e o Client é o dono (@JoinTable), basta atualizar a lista dele.
            // O JPA vai gerenciar a tabela de junção tb_client_company automaticamente.
            client.setCompanies(new HashSet<>(companiesToLink));
        }

        Client saved = clientRepository.save(client);
        return clientMapper.toDto(saved);
    }

    /**
     * Remove um cliente pelo ID.
     * Verifica integridade referencial antes da deleção.
     *
     * @param id ID do cliente a ser removido
     * @throws ResourceNotFoundException       quando o cliente não é encontrado
     * @throws DataIntegrityViolationException quando existem dependências que impedem a deleção
     */
    @Transactional
    public void delete(Long id) {
        // 1. Verifica se existe
        if (!clientRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cliente não encontrado com ID: " + id);
        }

        // 2. VERIFICAÇÃO DE INTEGRIDADE (Solução Proativa)
        // Verifica se existem empresas vinculadas a este cliente antes de tentar deletar
        if (companyRepository.existsByClientsId(id)) {
            throw new DataIntegrityViolationException("Não é possível excluir o cliente. Existem empresas vinculadas a ele.");
        }

        // 3. Tenta deletar (Solução Reativa / Fallback)
        try {
            clientRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            // Caso alguma outra constraint passe despercebida pela verificação acima
            throw new DataIntegrityViolationException("Erro de integridade: O cliente possui registros dependentes e não pode ser excluído.");
        }
    }
    
    
    // Helpers

    /**
     * Converte uma entidade Cliente para DTO.
     *
     * @param client Entidade cliente a ser convertida
     * @return DTO com dados básicos do cliente
     */
    private ClientDTO mapToResponseDto(Client client) {
        ClientDTO dto = new ClientDTO();
        dto.setId(client.getId());
        dto.setName(client.getName());
        dto.setEmail(client.getEmail());
        dto.setCompanyNames(client.getCompanies().stream().map(Company::getName).toList());
        dto.setCompanyIds(client.getCompanies().stream().map(Company::getId).toList());
        return dto;
    }
}