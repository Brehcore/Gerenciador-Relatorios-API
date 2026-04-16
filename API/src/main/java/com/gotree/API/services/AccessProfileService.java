package com.gotree.API.services;

import com.gotree.API.dto.profile.AccessProfileRequestDTO;
import com.gotree.API.dto.profile.AccessProfileResponseDTO;
import com.gotree.API.entities.AccessProfile;
import com.gotree.API.repositories.AccessProfileRepository;
import com.gotree.API.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccessProfileService {

    private final AccessProfileRepository profileRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AccessProfileResponseDTO> getAllProfiles() {
        return profileRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AccessProfileResponseDTO getProfileById(Long id) {
        AccessProfile profile = profileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Perfil não encontrado com ID: " + id));
        return mapToResponseDTO(profile);
    }

    @Transactional
    public AccessProfileResponseDTO createProfile(AccessProfileRequestDTO dto) {
        if (profileRepository.existsByName(dto.getName())) {
            throw new RuntimeException("Já existe um perfil cadastrado com o nome: " + dto.getName());
        }

        AccessProfile profile = new AccessProfile();
        profile.setName(dto.getName());
        profile.setPermissions(dto.getPermissions());

        profile = profileRepository.save(profile);
        return mapToResponseDTO(profile);
    }

    @Transactional
    public AccessProfileResponseDTO updateProfile(Long id, AccessProfileRequestDTO dto) {
        AccessProfile profile = profileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Perfil não encontrado com ID: " + id));

        // Verifica se o nome foi alterado e se o novo nome já existe
        if (!profile.getName().equalsIgnoreCase(dto.getName()) && profileRepository.existsByName(dto.getName())) {
            throw new RuntimeException("Já existe um perfil cadastrado com o nome: " + dto.getName());
        }

        profile.setName(dto.getName());
        profile.setPermissions(dto.getPermissions());

        profile = profileRepository.save(profile);
        return mapToResponseDTO(profile);
    }

    @Transactional
    public void deleteProfile(Long profileId) {
        AccessProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Perfil não encontrado com ID: " + profileId));

        // Verifica se existe algum usuário usando este perfil (A TRAVA DE SEGURANÇA!)
        boolean hasUsers = userRepository.existsByProfileId(profileId);
        if (hasUsers) {
            throw new RuntimeException("Não é possível excluir o perfil '" + profile.getName() + "', pois existem usuários vinculados a ele.");
        }

        profileRepository.deleteById(profileId);
    }

    // Método auxiliar de mapeamento (Se preferir, pode usar o MapStruct)
    private AccessProfileResponseDTO mapToResponseDTO(AccessProfile profile) {
        AccessProfileResponseDTO dto = new AccessProfileResponseDTO();
        dto.setId(profile.getId());
        dto.setName(profile.getName());
        dto.setPermissions(profile.getPermissions());
        return dto;
    }
}