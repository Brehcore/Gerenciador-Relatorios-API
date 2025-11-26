package com.gotree.API.services;

import com.gotree.API.dto.document.DocumentSummaryDTO;
import com.gotree.API.entities.*;
import com.gotree.API.repositories.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Serviço responsável por agregar e gerenciar diferentes tipos de documentos no sistema.
 */
@Service
public class DocumentAggregationService {

    private final TechnicalVisitRepository technicalVisitRepository;
    private final TechnicalVisitService technicalVisitService;
    private final AepService aepService;
    private final AepReportRepository aepReportRepository;
    private final RiskChecklistService riskChecklistService;
    private final OccupationalRiskReportRepository riskReportRepository;

    @Value("${file.storage.path}")
    private String fileStoragePath;

    public DocumentAggregationService(TechnicalVisitRepository technicalVisitRepository,
                                      TechnicalVisitService technicalVisitService,
                                      AepService aepService, AepReportRepository aepReportRepository,
                                      RiskChecklistService riskChecklistService, OccupationalRiskReportRepository riskReportRepository) {
        this.technicalVisitRepository = technicalVisitRepository;
        this.technicalVisitService = technicalVisitService;
        this.aepService = aepService;
        this.aepReportRepository = aepReportRepository;
        this.riskChecklistService = riskChecklistService;
        this.riskReportRepository = riskReportRepository;
    }

    // ===================================================================================
    // MÉTODOS DE BUSCA (Refatorados para evitar duplicação)
    // ===================================================================================

    /**
     * Método PRIVADO centralizador que busca, mapeia e ordena TODOS os documentos do usuário.
     * Evita repetir a lógica de consulta nos métodos públicos.
     */
    private List<DocumentSummaryDTO> fetchAllSortedDocuments(User technician) {
        // 1. Relatórios de Visita Técnica
        List<DocumentSummaryDTO> technicalVisits = technicalVisitRepository.findAllWithCompanyByTechnician(technician)
                .stream()
                .map(this::mapVisitToSummaryDto)
                .toList();

        // 2. Relatórios AEP
        List<DocumentSummaryDTO> aepReports = aepReportRepository.findAllByEvaluator(technician)
                .stream()
                .map(this::mapAepToSummaryDto)
                .toList();

        // 3. Checklist de Riscos
        List<DocumentSummaryDTO> riskReports = riskReportRepository.findByTechnicianOrderByInspectionDateDesc(technician)
                .stream()
                .map(this::mapRiskToSummaryDto)
                .toList();

        // 4. Junta tudo em uma lista só
        List<DocumentSummaryDTO> allDocuments = Stream.of(technicalVisits, aepReports, riskReports)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        // 5. Ordena por data (Decrescente - mais recente primeiro)
        allDocuments.sort(Comparator.comparing(DocumentSummaryDTO::getCreationDate, Comparator.nullsLast(Comparator.reverseOrder())));

        return allDocuments;
    }

    /**
     * Recupera documentos COM filtros e paginação (Usado na tela de listagem 'Meus Documentos').
     */
    @Transactional(readOnly = true)
    public Page<DocumentSummaryDTO> findAllDocumentsForUser(
            User technician,
            String typeFilter, String clientFilter,
            LocalDate startDate, LocalDate endDate,
            Pageable pageable
    ) {
        // 1. Chama o método auxiliar (REMOVIDA A DUPLICAÇÃO)
        List<DocumentSummaryDTO> allDocs = fetchAllSortedDocuments(technician);

        // Transforma em Stream para aplicar filtros
        Stream<DocumentSummaryDTO> stream = allDocs.stream();

        // 2. Filtro por TIPO
        if (typeFilter != null && !typeFilter.isBlank()) {
            final String typeInput = typeFilter.trim();
            stream = stream.filter(doc -> {
                if ("visit".equalsIgnoreCase(typeInput)) return "Relatório de Visita".equals(doc.getDocumentType());
                if ("aep".equalsIgnoreCase(typeInput)) return "Avaliação Ergonômica Preliminar".equals(doc.getDocumentType());
                if ("risk".equalsIgnoreCase(typeInput)) return "Checklist de Riscos".equals(doc.getDocumentType());
                return false;
            });
        }

        // 3. Filtro por NOME DO CLIENTE
        if (clientFilter != null && !clientFilter.isBlank()) {
            String filter = clientFilter.toLowerCase().trim();
            stream = stream.filter(doc ->
                    doc.getClientName() != null &&
                            doc.getClientName().toLowerCase().contains(filter)
            );
        }

        // 4. Filtro por DATA
        if (startDate != null) {
            stream = stream.filter(doc -> doc.getCreationDate() != null && !doc.getCreationDate().isBefore(startDate));
        }
        if (endDate != null) {
            stream = stream.filter(doc -> doc.getCreationDate() != null && !doc.getCreationDate().isAfter(endDate));
        }

        // 5. Coleta a lista filtrada
        List<DocumentSummaryDTO> filteredList = stream.collect(Collectors.toList());

        // 6. Paginação Manual (Em memória)
        long totalElements = filteredList.size();
        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int startItem = currentPage * pageSize;

        List<DocumentSummaryDTO> paginatedList;

        if (startItem >= totalElements) {
            paginatedList = Collections.emptyList();
        } else {
            int toIndex = Math.min(startItem + pageSize, (int) totalElements);
            paginatedList = filteredList.subList(startItem, toIndex);
        }

        return new PageImpl<>(paginatedList, pageable, totalElements);
    }

    /**
     * Retorna a LISTA COMPLETA de documentos para um usuário (sem paginação).
     * Usado pelo Dashboard para calcular estatísticas.
     */
    @Transactional(readOnly = true)
    public List<DocumentSummaryDTO> findAllDocumentsListForUser(User technician) {
        // Reutiliza o método auxiliar (sem filtros)
        return fetchAllSortedDocuments(technician);
    }

    /**
     * Recupera os 5 documentos mais recentes associados a um técnico (Widget do Dashboard).
     */
    @Transactional(readOnly = true)
    public List<DocumentSummaryDTO> findLatestDocumentsForUser(User technician) {
        // Reutiliza o método auxiliar e limita a 5
        return fetchAllSortedDocuments(technician).stream()
                .limit(5)
                .collect(Collectors.toList());
    }

    /**
     * Recupera os 5 documentos mais recentes do sistema (Visão Admin).
     * OBS: Este método busca de TODOS os usuários, por isso mantém a lógica própria.
     */
    @Transactional(readOnly = true)
    public List<DocumentSummaryDTO> findAllLatestDocumentsForAdmin() {
        List<DocumentSummaryDTO> technicalVisits = technicalVisitRepository.findAll()
                .stream().map(this::mapVisitToSummaryDto).toList();

        List<DocumentSummaryDTO> aepReports = aepReportRepository.findAll()
                .stream().map(this::mapAepToSummaryDto).toList();

        List<DocumentSummaryDTO> riskReports = riskReportRepository.findAll()
                .stream().map(this::mapRiskToSummaryDto).toList();

        List<DocumentSummaryDTO> allDocuments = Stream.of(technicalVisits, aepReports, riskReports)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        allDocuments.sort(Comparator.comparing(DocumentSummaryDTO::getCreationDate, Comparator.nullsLast(Comparator.reverseOrder())));
        return allDocuments.stream().limit(5).collect(Collectors.toList());
    }

    // ===================================================================================
    // MÉTODOS DE ARQUIVO (PDF) E DELEÇÃO
    // ===================================================================================

    public byte[] loadPdfFileByTypeAndId(String type, Long id, User currentUser) throws IOException {
        String fileName = null;
        byte[] pdfBytes = null;

        if ("visit".equalsIgnoreCase(type)) {
            TechnicalVisit visit = technicalVisitRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Relatório de Visita com ID " + id + " não encontrado."));
            fileName = visit.getPdfPath();

        } else if ("aep".equalsIgnoreCase(type)) {
            pdfBytes = aepService.loadOrGenerateAepPdf(id, currentUser);

        } else if ("risk".equalsIgnoreCase(type)) {
            OccupationalRiskReport report = riskReportRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Relatório não encontrado."));
            fileName = report.getPdfPath();

        } else {
            throw new IllegalArgumentException("Tipo de documento inválido: " + type);
        }

        if (pdfBytes != null) {
            return pdfBytes;
        }

        if (fileName == null || fileName.isBlank()) {
            throw new RuntimeException("Este documento não possui um PDF associado.");
        }

        Path path;
        if ("visit".equalsIgnoreCase(type)) {
            path = Paths.get(fileStoragePath, fileName);
        } else if ("risk".equalsIgnoreCase(type)) {
            path = Paths.get(fileName);
        } else {
            throw new IOException("Lógica de caminho de PDF não definida para o tipo: " + type);
        }

        if (!Files.exists(path)) {
            throw new IOException("Arquivo PDF não encontrado no caminho: " + path);
        }
        return Files.readAllBytes(path);
    }

    @Transactional
    public void deleteDocumentByTypeAndId(String type, Long id, User currentUser) {
        if ("visit".equalsIgnoreCase(type)) {
            technicalVisitService.deleteVisit(id, currentUser);
        } else if ("aep".equalsIgnoreCase(type)) {
            aepService.deleteAepReport(id, currentUser);
        } else if ("risk".equalsIgnoreCase(type)) {
            riskChecklistService.deleteReport(id, currentUser);
        } else {
            throw new IllegalArgumentException("Tipo de documento inválido: " + type);
        }
    }

    // ===================================================================================
    // HELPERS E MAPPERS (Com lógica de E-mail e Cliente)
    // ===================================================================================

    public DocumentSummaryDTO mapVisitToSummaryDto(TechnicalVisit visit) {
        DocumentSummaryDTO dto = new DocumentSummaryDTO();
        dto.setId(visit.getId());
        dto.setDocumentType("Relatório de Visita");
        dto.setTitle(visit.getTitle());
        dto.setCreationDate(visit.getVisitDate());

        // Usa o helper passando a empresa correta (clientCompany)
        fillCommonFields(dto, visit.getClientCompany(), visit.getSentToClientAt(), visit.getTechnicianSignatureImageBase64());

        return dto;
    }

    public DocumentSummaryDTO mapAepToSummaryDto(AepReport aep) {
        DocumentSummaryDTO dto = new DocumentSummaryDTO();
        dto.setId(aep.getId());
        dto.setDocumentType("Avaliação Ergonômica Preliminar");
        dto.setTitle(aep.getEvaluatedFunction());
        dto.setCreationDate(aep.getEvaluationDate());

        // AEP não tem assinatura no mesmo padrão, passamos null
        fillCommonFields(dto, aep.getCompany(), aep.getSentToClientAt(), null);

        return dto;
    }

    public DocumentSummaryDTO mapRiskToSummaryDto(OccupationalRiskReport report) {
        DocumentSummaryDTO dto = new DocumentSummaryDTO();
        dto.setId(report.getId());
        dto.setDocumentType("Checklist de Riscos");
        dto.setTitle(report.getTitle());
        dto.setCreationDate(report.getInspectionDate());

        fillCommonFields(dto, report.getCompany(), report.getSentToClientAt(), report.getTechnicianSignatureImageBase64());

        return dto;
    }

    /**
     * Helper Privado: Centraliza a lógica de Cliente, Email, Status de Envio e Assinatura.
     *
     * @param dto O DTO a ser preenchido
     * @param company A empresa associada ao documento
     * @param sentAt A data/hora de envio (pode ser null)
     * @param signatureBase64 A assinatura (para definir o status signed)
     */
    private void fillCommonFields(DocumentSummaryDTO dto, Company company, LocalDateTime sentAt, String signatureBase64) {
        // 1. Lógica da Empresa e E-mail do Cliente
        if (company != null) {
            dto.setClientName(company.getName());

            // Navega para buscar o e-mail do cliente vinculado
            if (company.getClient() != null) {
                dto.setClientEmail(company.getClient().getEmail());
            } else {
                dto.setClientEmail(null); // Botão ficará cinza no frontend
            }
        } else {
            dto.setClientName("N/A");
            dto.setClientEmail(null);
        }

        // 2. Lógica de Status de Envio (Verde/Vermelho)
        dto.setEmailSent(sentAt != null);

        // 3. Lógica de Assinatura (Cadeado)
        dto.setSigned(signatureBase64 != null && !signatureBase64.isBlank());
    }
}