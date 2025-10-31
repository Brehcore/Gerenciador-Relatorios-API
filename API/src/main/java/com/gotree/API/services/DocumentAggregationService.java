package com.gotree.API.services;

import com.gotree.API.dto.document.DocumentSummaryDTO;
import com.gotree.API.entities.InspectionReport;
import com.gotree.API.entities.TechnicalVisit;
import com.gotree.API.entities.User;
import com.gotree.API.repositories.InspectionReportRepository;
import com.gotree.API.repositories.TechnicalVisitRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DocumentAggregationService {

    private final InspectionReportRepository inspectionReportRepository;
    private final TechnicalVisitRepository technicalVisitRepository;
    private final InspectionReportService inspectionReportService;
    private final TechnicalVisitService technicalVisitService;
    private final AepService aepService;

    @Value("${file.storage.path}") // Injete o caminho base aqui também
    private String fileStoragePath;

    public DocumentAggregationService(InspectionReportRepository inspectionReportRepository,
                                      TechnicalVisitRepository technicalVisitRepository,
                                      InspectionReportService inspectionReportService,
                                      TechnicalVisitService technicalVisitService,
                                      AepService aepService) {
        this.inspectionReportRepository = inspectionReportRepository;
        this.technicalVisitRepository = technicalVisitRepository;
        this.inspectionReportService = inspectionReportService;
        this.technicalVisitService = technicalVisitService;
        this.aepService = aepService;
    }

    @Transactional(readOnly = true)
    public List<DocumentSummaryDTO> findAllDocumentsForUser(User technician) {
        // Bloco 1: Checklists de Inspeção
        List<DocumentSummaryDTO> inspectionReports = inspectionReportRepository.findAllWithCompanyByTechnician(technician)
                .stream()
                .map(report -> {
                    DocumentSummaryDTO dto = new DocumentSummaryDTO();
                    dto.setId(report.getId());
                    dto.setDocumentType("Checklist de Inspeção");
                    dto.setTitle(report.getTitle());
                    dto.setClientName(report.getCompany() != null ? report.getCompany().getName() : "N/A");
                    dto.setCreationDate(report.getInspectionDate());
                    return dto; // CORREÇÃO: Adicionado o 'return'
                })
                .toList();

        // Bloco 2: Relatórios de Visita Técnica
        List<DocumentSummaryDTO> technicalVisits = technicalVisitRepository.findAllWithCompanyByTechnician(technician)
                .stream()
                .map(visit -> {
                    DocumentSummaryDTO dto = new DocumentSummaryDTO();
                    dto.setId(visit.getId());
                    dto.setDocumentType("Relatório de Visita");
                    dto.setTitle(visit.getTitle());
                    dto.setClientName(visit.getClientCompany() != null ? visit.getClientCompany().getName() : "N/A");
                    dto.setCreationDate(visit.getVisitDate());
                    return dto; // CORREÇÃO: Adicionado o 'return'
                })
                .toList();

        // 3. Junta todas as listas numa só
        List<DocumentSummaryDTO> allDocuments = Stream.concat(inspectionReports.stream(), technicalVisits.stream())
                .collect(Collectors.toList());

        // 4. Ordena a lista final pela data de criação, dos mais recentes para os mais antigos
        allDocuments.sort(Comparator.comparing(DocumentSummaryDTO::getCreationDate).reversed());

        return allDocuments;
    }

    @Transactional(readOnly = true)
    public List<DocumentSummaryDTO> findLatestDocumentsForUser(User technician) {
        List<DocumentSummaryDTO> allDocuments = findAllDocumentsForUser(technician);

        // Retorna apenas os 5 primeiros da lista já ordenada
        return allDocuments.stream().limit(5).collect(Collectors.toList());
    }

    public byte[] loadPdfFileByTypeAndId(String type, Long id) throws IOException {
        String fileName;

        // Usamos um 'if/else if' para decidir qual repositório usar
        // "checklist" e "visit" são exemplos, use os nomes que fizerem sentido para você
        if ("checklist".equalsIgnoreCase(type)) {
            InspectionReport report = inspectionReportRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Checklist com ID " + id + " não encontrado."));
            fileName = report.getPdfPath();

        } else if ("visit".equalsIgnoreCase(type)) {
            TechnicalVisit visit = technicalVisitRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Relatório de Visita com ID " + id + " não encontrado."));
            fileName = visit.getPdfPath();

        } else {
            throw new IllegalArgumentException("Tipo de documento inválido: " + type);
        }

        if (fileName == null || fileName.isBlank()) {
            throw new RuntimeException("Este documento não possui um PDF associado.");
        }

        // A lógica de reconstrução do caminho que já corrigimos continua a mesma
        Path path = Paths.get(fileStoragePath, fileName);
        if (!Files.exists(path)) {
            throw new IOException("Arquivo PDF não encontrado no caminho: " + path);
        }
        return Files.readAllBytes(path);
    }

    @Transactional
    public void deleteDocumentByTypeAndId(String type, Long id, User currentUser) {
        // Usamos um 'if/else if' para ir ao serviço correto, sem ambiguidade
        if ("checklist".equalsIgnoreCase(type)) {
            // A lógica de deleção (incluindo verificação de segurança) já está no serviço
            inspectionReportService.deleteReport(id, currentUser);

        } else if ("visit".equalsIgnoreCase(type)) {
            // A mesma lógica para o serviço de visita técnica
            technicalVisitService.deleteVisit(id, currentUser);

        } else if ("aep".equalsIgnoreCase(type)) {
            // A mesma lógica para o serviço da AEP
            aepService.deleteAepReport(id, currentUser);

        } else {
            // Se o tipo for desconhecido, lançamos um erro
            throw new IllegalArgumentException("Tipo de documento inválido para deleção: " + type);
        }
    }
}