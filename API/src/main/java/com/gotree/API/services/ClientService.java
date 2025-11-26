package com.gotree.API.services;

import com.gotree.API.dto.client.ClientDTO;
import com.gotree.API.entities.Client;
import com.gotree.API.entities.Company;
import com.gotree.API.mappers.ClientMapper;
import com.gotree.API.repositories.ClientRepository;
import com.gotree.API.repositories.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        // Usa o mapper para converter a lista
        return clientMapper.toDtoList(clientRepository.findAll());
    }

    @Transactional(readOnly = true)
    public ClientDTO findById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        // Usa o mapper (atenção ao camelCase 'toDto')
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

        // Lógica de Vínculo: Se vieram IDs de empresas, atualizamos
        if (dto.getCompanyIds() != null) {
            // 1. Limpa vínculos antigos (opcional, depende da regra de negócio)
            if (client.getCompanies() != null) {
                for (Company c : client.getCompanies()) {
                    c.setClient(null); // Desvincula
                }
            }

            // 2. Busca as novas empresas e vincula
            List<Company> companiesToLink = companyRepository.findAllById(dto.getCompanyIds());
            for (Company c : companiesToLink) {
                c.setClient(client); // Vincula ao cliente
            }
            client.setCompanies(companiesToLink);
        }

        Client saved = clientRepository.save(client);

        // Retorna usando o mapper, que já preenche companyIds e companyNames automaticamente
        return clientMapper.toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        // Segurança: Desvincula as empresas antes de deletar o cliente
        for (Company c : client.getCompanies()) {
            c.setClient(null);
        }

        clientRepository.delete(client);
    }
}