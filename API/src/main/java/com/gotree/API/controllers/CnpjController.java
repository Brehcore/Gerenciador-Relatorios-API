package com.gotree.API.controllers;

import com.gotree.API.dto.external.CnpjResponseDTO;
import com.gotree.API.services.CnpjService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/external/cnpj")
public class CnpjController {

    private final CnpjService cnpjService;

    public CnpjController(CnpjService cnpjService) {
        this.cnpjService = cnpjService;
    }

    @GetMapping("/{cnpj}")
    public ResponseEntity<CnpjResponseDTO> getCompanyInfo(@PathVariable String cnpj) {
        CnpjResponseDTO response = cnpjService.consultCnpj(cnpj);
        return ResponseEntity.ok(response);
    }
}