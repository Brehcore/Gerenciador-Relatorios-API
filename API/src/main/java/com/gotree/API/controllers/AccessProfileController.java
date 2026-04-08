package com.gotree.API.controllers;

import com.gotree.API.enums.SystemPermission;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/profiles")
@PreAuthorize( "hasRole('ADMIN')")
public class AccessProfileController {

    @GetMapping("/permissions")
    @Operation(summary = "Lista todas as permissões do sistema para montar os checkboxes")
    public ResponseEntity<List<SystemPermission>> getAllPermissions() {
        // Retorna todos os valores do Enum
        return ResponseEntity.ok(Arrays.asList(SystemPermission.values()));
    }

    // POST, PUT, DELETE, GET para o AccessProfile...
}