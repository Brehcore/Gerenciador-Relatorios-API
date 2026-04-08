package com.gotree.API.services;

import com.gotree.API.repositories.AccessProfileRepository;
import com.gotree.API.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessProfileService {

    private final AccessProfileRepository profileRepository;
    private final UserRepository userRepository;

    public void deleteProfile(Long profileId) {
        // Verifica se existe algum usuário usando este perfil
        boolean hasUsers = userRepository.existsByAccessProfileId(profileId);

        if (hasUsers) {
            throw new RuntimeException("Não é possível excluir este perfil, pois existem usuários vinculados a ele.");
        }

        profileRepository.deleteById(profileId);
    }
}
