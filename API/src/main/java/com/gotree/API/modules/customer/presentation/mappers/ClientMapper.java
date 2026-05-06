package com.gotree.API.modules.customer.presentation.mappers;

import com.gotree.API.modules.customer.presentation.dto.ClientDTO;
import com.gotree.API.modules.customer.domain.entities.Client;
import com.gotree.API.modules.administration.domain.entities.Company;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.List;
import java.util.Set; // Import necessário
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    @Mapping(target = "companyIds", expression = "java(mapCompaniesToIds(client.getCompanies()))")
    @Mapping(target = "companyNames", expression = "java(mapCompaniesToNames(client.getCompanies()))")
    ClientDTO toDto(Client client);

    @Mapping(target = "companies", ignore = true)
    @Mapping(target = "id", ignore = true)
    Client toEntity(ClientDTO dto);

    List<ClientDTO> toDtoList(List<Client> clients);

    // HELPERS

    default List<Long> mapCompaniesToIds(Set<Company> companies) {
        if (companies == null) return Collections.emptyList();
        return companies.stream()
                .map(Company::getId)
                .collect(Collectors.toList());
    }

    default List<String> mapCompaniesToNames(Set<Company> companies) {
        if (companies == null) return Collections.emptyList();
        return companies.stream()
                .map(Company::getName)
                .collect(Collectors.toList());
    }
}