package com.gotree.API.mappers;

import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.gotree.API.dto.user.UserRequestDTO;
import com.gotree.API.dto.user.UserResponseDTO;
import com.gotree.API.entities.User;

import br.com.caelum.stella.format.CPFFormatter;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", expression = "java(com.gotree.API.enums.UserRole.USER)")
    @Mapping(target = "authorities", ignore = true)
    // Ignora campos de certificado na criação/update via DTO simples (são gerenciados por endpoints específicos)
    @Mapping(target = "certificatePath", ignore = true)
    @Mapping(target = "certificatePassword", ignore = true)
    @Mapping(target = "certificateValidity", ignore = true)
    @Mapping(target = "passwordResetRequired", ignore = true)
    User toEntity(UserRequestDTO dto);

    // --- LÓGICA DO CERTIFICADO ---
    // 1. certificateValidity: Mapeado automaticamente pois tem o mesmo nome na Entity e no DTO.
    // 2. hasCertificate: Calculado via expressão Java (se tem path, tem certificado).
    @Mapping(target = "hasCertificate", expression = "java(user.getCertificatePath() != null && !user.getCertificatePath().isEmpty())")
    UserResponseDTO toDto(User user);

    List<UserResponseDTO> toDtoList(List<User> user);

    // Só formata quando vai gravar (DTO -> Entity)
    @AfterMapping
    default void formatCpfAfterToEntity(@MappingTarget User user) {
        if (user.getCpf() != null) {
            String cleanCpf = user.getCpf().replaceAll("[^\\d]", "");

            // Validação simples para evitar erro no formatter se o CPF estiver incompleto
            if (cleanCpf.length() == 11) {
                try {
                    CPFFormatter formatter = new CPFFormatter();
                    user.setCpf(formatter.format(cleanCpf));
                } catch (Exception e) {
                    // Se falhar a formatação, mantém apenas os números
                    user.setCpf(cleanCpf);
                }
            } else {
                user.setCpf(cleanCpf);
            }
        }
    }
}