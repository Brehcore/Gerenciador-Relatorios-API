package com.gotree.API.controllers;


import com.gotree.API.dto.report.SaveInspectionReportRequestDTO;
import com.gotree.API.entities.InspectionReport;
import com.gotree.API.services.InspectionReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping
public class ReportController {

    private final InspectionReportService inspectionReportService;

    public ReportController(InspectionReportService inspectionReportService) {
        this.inspectionReportService = inspectionReportService;
    }

    // --- ENDPOINT PARA SALVAR O CHECKLIST ---
    // Este é o endpoint que o 'formsPage' chamará após coletar as assinaturas.
    @PostMapping("/inspection-reports")
    public ResponseEntity<?> saveInspectionReport(@Valid @RequestBody SaveInspectionReportRequestDTO dto,
                                                  Authentication authentication) {
        // Chama o serviço para salvar o relatório no banco e gerar/salvar o PDF
        InspectionReport savedReport = inspectionReportService.saveReportAndGeneratePdf(dto, authentication);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Relatório salvo com sucesso!");
        response.put("reportId", savedReport.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}