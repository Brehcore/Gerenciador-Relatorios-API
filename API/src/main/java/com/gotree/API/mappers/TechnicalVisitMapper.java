package com.gotree.API.mappers;

import com.gotree.API.dto.visit.TechnicalVisitResponseDTO;
import com.gotree.API.entities.TechnicalVisit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TechnicalVisitMapper {

    /**
     * Mapeia a entidade TechnicalVisit para o DTO de resposta.
     * A anotação @Mapping ensina o mapper a pegar o nome da empresa
     * de dentro do objeto aninhado 'clientCompany'.
     */
    @Mapping(source = "clientCompany.name", target = "clientCompanyName")
    @Mapping(source = "id", target = "id") // Mapeamento explícito para o título, se necessário
    TechnicalVisitResponseDTO toDto(TechnicalVisit technicalVisit);

    /**
     * Mapeia uma lista de entidades TechnicalVisit para uma lista de DTOs.
     */
    List<TechnicalVisitResponseDTO> toDtoList(List<TechnicalVisit> technicalVisits);
}