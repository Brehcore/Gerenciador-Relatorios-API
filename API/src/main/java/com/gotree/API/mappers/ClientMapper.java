package com.gotree.API.mappers;

import com.gotree.API.dto.client.ClientDTO;
import com.gotree.API.entities.Client;
import com.gotree.API.entities.Company;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    /**
     * Converte Entidade -> DTO.
     * Usamos expressões Java para converter a lista de Objetos Company
     * em listas simples de IDs e Nomes para o Frontend.
     */
    @Mapping(target = "companyIds", expression = "java(mapCompaniesToIds(client.getCompanies()))")
    @Mapping(target = "companyNames", expression = "java(mapCompaniesToNames(client.getCompanies()))")
    ClientDTO toDto(Client client);

    /**
     * Converte DTO -> Entidade.
     * IMPORTANTE: Ignoramos a lista 'companies' aqui.
     * O DTO traz apenas IDs (companyIds). A busca no banco e o vínculo
     * dos objetos Company reais devem ser feitos no ClientService.
     */
    @Mapping(target = "companies", ignore = true)
    @Mapping(target = "id", ignore = true) // O ID geralmente é gerado pelo banco ou tratado no update
    Client toEntity(ClientDTO dto);

    List<ClientDTO> toDtoList(List<Client> clients);

    // --- MÉTODOS AUXILIARES (Helpers) ---

    default List<Long> mapCompaniesToIds(List<Company> companies) {
        if (companies == null) return Collections.emptyList();
        return companies.stream()
                .map(Company::getId)
                .collect(Collectors.toList());
    }

    default List<String> mapCompaniesToNames(List<Company> companies) {
        if (companies == null) return Collections.emptyList();
        return companies.stream()
                .map(Company::getName)
                .collect(Collectors.toList());
    }
}