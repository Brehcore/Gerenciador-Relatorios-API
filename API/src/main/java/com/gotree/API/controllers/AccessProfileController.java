package com.gotree.API.controllers;

import com.gotree.API.dto.profile.AccessProfileRequestDTO;
import com.gotree.API.dto.profile.AccessProfileResponseDTO;
import com.gotree.API.enums.SystemPermission;
import com.gotree.API.services.AccessProfileService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/profiles")
@RequiredArgsConstructor
public class AccessProfileController {

    private final AccessProfileService profileService;

    @GetMapping("/permissions")
    @Operation(summary = "Lista todas as permissões do sistema para montar os checkboxes")
    @PreAuthorize("hasAuthority('VIEW_USERS') or hasRole('ADMIN')")
    public ResponseEntity<List<SystemPermission>> getAllPermissions() {
        return ResponseEntity.ok(Arrays.asList(SystemPermission.values()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_USERS') or hasRole('ADMIN')")
    @Operation(summary = "Lista todos os perfis de acesso cadastrados")
    public ResponseEntity<List<AccessProfileResponseDTO>> getAllProfiles() {
        return ResponseEntity.ok(profileService.getAllProfiles());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_USERS') or hasRole('ADMIN')")
    @Operation(summary = "Busca um perfil de acesso pelo ID")
    public ResponseEntity<AccessProfileResponseDTO> getProfileById(@PathVariable Long id) {
        return ResponseEntity.ok(profileService.getProfileById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_USERS') or hasRole('ADMIN')")
    @Operation(summary = "Cria um novo perfil de acesso")
    public ResponseEntity<AccessProfileResponseDTO> createProfile(@Valid @RequestBody AccessProfileRequestDTO dto) {
        AccessProfileResponseDTO response = profileService.createProfile(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDIT_USERS') or hasRole('ADMIN')")
    @Operation(summary = "Atualiza um perfil de acesso existente")
    public ResponseEntity<AccessProfileResponseDTO> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody AccessProfileRequestDTO dto) {
        return ResponseEntity.ok(profileService.updateProfile(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_USERS') or hasRole('ADMIN')")
    @Operation(summary = "Exclui um perfil de acesso (se não houver usuários vinculados)")
    public ResponseEntity<Void> deleteProfile(@PathVariable Long id) {
        profileService.deleteProfile(id);
        return ResponseEntity.noContent().build();
    }
}