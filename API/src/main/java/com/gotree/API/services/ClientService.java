package com.gotree.API.services;

import com.gotree.API.dto.client.ClientDTO;
import com.gotree.API.entities.Client;
import com.gotree.API.entities.Company;
import com.gotree.API.exceptions.ResourceNotFoundException;
import com.gotree.API.mappers.ClientMapper;
import com.gotree.API.repositories.ClientRepository;
import com.gotree.API.repositories.CompanyRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final CompanyRepository companyRepository;
    private final ClientMapper clientMapper;

    public ClientService(ClientRepository clientRepository,
                         CompanyRepository companyRepository,
                         ClientMapper clientMapper) {
        this.clientRepository = clientRepository;
        this.companyRepository = companyRepository;
        this.clientMapper = clientMapper;
    }

    @Transactional(readOnly = true)
    public List<ClientDTO> findAll() {
        return clientMapper.toDtoList(clientRepository.findAll());
    }

    @Transactional(readOnly = true)
    public ClientDTO findById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        return clientMapper.toDto(client);
    }

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
}