package com.gotree.API.services;

import com.gotree.API.dto.document.DocumentSummaryDTO;
import com.gotree.API.dto.document.FileDownloadDTO;
import com.gotree.API.entities.*;
import com.gotree.API.repositories.*;
import jakarta.servlet.http.HttpServletResponse;
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
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class DocumentAggregationService {

    private final TechnicalVisitRepository technicalVisitRepository;
    private final TechnicalVisitService technicalVisitService;
    private final AepService aepService;
    private final EmailService emailService;
    private final AepReportRepository aepReportRepository;
    private final RiskChecklistService riskChecklistService;
    private final OccupationalRiskReportRepository riskReportRepository;

    @Value("${file.storage.path}")
    private String fileStoragePath;

    public DocumentAggregationService(TechnicalVisitRepository technicalVisitRepository,
                                      TechnicalVisitService technicalVisitService,
                                      AepService aepService, EmailService emailService, AepReportRepository aepReportRepository,
                                      RiskChecklistService riskChecklistService, OccupationalRiskReportRepository riskReportRepository) {
        this.technicalVisitRepository = technicalVisitRepository;
        this.technicalVisitService = technicalVisitService;
        this.aepService = aepService;
        this.emailService = emailService;
        this.aepReportRepository = aepReportRepository;
        this.riskChecklistService = riskChecklistService;
        this.riskReportRepository = riskReportRepository;
    }

    // ===================================================================================
    // 1. MÉTODOS PÚBLICOS (ENTRADA)
    // ===================================================================================

    /**
     * TÉCNICO: Recupera documentos COM filtros e paginação.
     */
    @Transactional(readOnly = true)
    public Page<DocumentSummaryDTO> findAllDocumentsForUser(
            User technician,
            String typeFilter, String clientFilter,
            LocalDate startDate, LocalDate endDate,
            Pageable pageable
    ) {
        // 1. Busca dados específicos do técnico
        List<DocumentSummaryDTO> rawDocs = fetchRawDocumentsByTechnician(technician);

        // 2. Aplica a lógica comum de filtro/paginação
        return processDocumentList(rawDocs, typeFilter, clientFilter, startDate, endDate, pageable);
    }

    /**
     * ADMIN: Recupera TODOS os documentos do sistema, com filtros e paginação.
     */
    @Transactional(readOnly = true)
    public Page<DocumentSummaryDTO> findAllDocumentsGlobal(
            String typeFilter, String clientFilter,
            LocalDate startDate, LocalDate endDate,
            Pageable pageable
    ) {
        // 1. Busca dados globais
        List<DocumentSummaryDTO> rawDocs = fetchRawDocumentsGlobal();

        // 2. Aplica a MESMA lógica comum
        return processDocumentList(rawDocs, typeFilter, clientFilter, startDate, endDate, pageable);
    }

    /**
     * DASHBOARD: Retorna a LISTA COMPLETA ordenada (sem paginação).
     */
    @Transactional(readOnly = true)
    public List<DocumentSummaryDTO> findAllDocumentsListForUser(User technician) {
        List<DocumentSummaryDTO> docs = fetchRawDocumentsByTechnician(technician);
        // Apenas ordena
        docs.sort(Comparator.comparing(DocumentSummaryDTO::getCreationDate, Comparator.nullsLast(Comparator.reverseOrder())));
        return docs;
    }

    /**
     * WIDGET: Recupera os 5 mais recentes.
     */
    @Transactional(readOnly = true)
    public List<DocumentSummaryDTO> findLatestDocumentsForUser(User technician) {
        List<DocumentSummaryDTO> docs = fetchRawDocumentsByTechnician(technician);
        docs.sort(Comparator.comparing(DocumentSummaryDTO::getCreationDate, Comparator.nullsLast(Comparator.reverseOrder())));
        return docs.stream().limit(5).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocumentSummaryDTO> findAllLatestDocumentsForAdmin() {
        List<DocumentSummaryDTO> docs = fetchRawDocumentsGlobal();
        docs.sort(Comparator.comparing(DocumentSummaryDTO::getCreationDate, Comparator.nullsLast(Comparator.reverseOrder())));
        return docs.stream().limit(5).collect(Collectors.toList());
    }

    // ===================================================================================
    // 2. FETCHERS (Busca de Dados Brutos)
    // ===================================================================================

    private List<DocumentSummaryDTO> fetchRawDocumentsByTechnician(User technician) {
        List<DocumentSummaryDTO> visits = technicalVisitRepository.findAllWithCompanyByTechnician(technician)
                .stream().map(this::mapVisitToSummaryDto).toList();

        List<DocumentSummaryDTO> aeps = aepReportRepository.findAllByEvaluator(technician)
                .stream().map(this::mapAepToSummaryDto).toList();

        List<DocumentSummaryDTO> risks = riskReportRepository.findByTechnicianOrderByInspectionDateDesc(technician)
                .stream().map(this::mapRiskToSummaryDto).toList();

        return Stream.of(visits, aeps, risks)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private List<DocumentSummaryDTO> fetchRawDocumentsGlobal() {
        List<DocumentSummaryDTO> visits = technicalVisitRepository.findAll()
                .stream().map(this::mapVisitToSummaryDto).toList();

        List<DocumentSummaryDTO> aeps = aepReportRepository.findAll()
                .stream().map(this::mapAepToSummaryDto).toList();

        List<DocumentSummaryDTO> risks = riskReportRepository.findAll()
                .stream().map(this::mapRiskToSummaryDto).toList();

        return Stream.of(visits, aeps, risks)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    // ===================================================================================
    // 3. PROCESSOR (Lógica Centralizada de Filtro e Paginação)
    // ===================================================================================

    private Page<DocumentSummaryDTO> processDocumentList(
            List<DocumentSummaryDTO> allDocuments,
            String typeFilter, String clientFilter,
            LocalDate startDate, LocalDate endDate,
            Pageable pageable
    ) {
        Stream<DocumentSummaryDTO> stream = allDocuments.stream();

        // 1. Filtro por TIPO
        if (typeFilter != null && !typeFilter.isBlank()) {
            final String typeInput = typeFilter.trim();
            stream = stream.filter(doc -> {
                if ("visit".equalsIgnoreCase(typeInput)) return "Relatório de Visita".equals(doc.getDocumentType());
                if ("aep".equalsIgnoreCase(typeInput)) return "Avaliação Ergonômica Preliminar".equals(doc.getDocumentType());
                if ("risk".equalsIgnoreCase(typeInput)) return "Checklist de Riscos".equals(doc.getDocumentType());
                return false;
            });
        }

        // 2. Filtro por NOME DO CLIENTE
        if (clientFilter != null && !clientFilter.isBlank()) {
            String filter = clientFilter.toLowerCase().trim();
            stream = stream.filter(doc ->
                    doc.getClientName() != null &&
                            doc.getClientName().toLowerCase().contains(filter)
            );
        }

        // 3. Filtro por DATA
        if (startDate != null) {
            stream = stream.filter(doc -> doc.getCreationDate() != null && !doc.getCreationDate().isBefore(startDate));
        }
        if (endDate != null) {
            stream = stream.filter(doc -> doc.getCreationDate() != null && !doc.getCreationDate().isAfter(endDate));
        }

        // 4. Coleta e Ordena
        List<DocumentSummaryDTO> filteredList = stream.sorted(Comparator.comparing(DocumentSummaryDTO::getCreationDate, Comparator.nullsLast(Comparator.reverseOrder()))).collect(Collectors.toList());

        // 5. Paginação Manual
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

    // ===================================================================================
    // 4. MÉTODOS DE ARQUIVO E DELEÇÃO (Mantidos iguais)
    // ===================================================================================

    public byte[] loadPdfFileByTypeAndId(String type, Long id, User currentUser) throws IOException {
        String fileName = null;
        byte[] pdfBytes = null;

        if ("visit".equalsIgnoreCase(type)) {
            TechnicalVisit visit = technicalVisitRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Relatório de Visita não encontrado."));
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

        if (pdfBytes != null) return pdfBytes;
        if (fileName == null || fileName.isBlank()) throw new RuntimeException("Este documento não possui um PDF associado.");

        Path path = ("visit".equalsIgnoreCase(type)) ? Paths.get(fileStoragePath, fileName) : Paths.get(fileName);

        if (!Files.exists(path)) throw new IOException("Arquivo PDF não encontrado.");
        return Files.readAllBytes(path);
    }

    @Transactional
    public void deleteDocumentByTypeAndId(String type, Long id, User currentUser) {
        if ("visit".equalsIgnoreCase(type)) technicalVisitService.deleteVisit(id, currentUser);
        else if ("aep".equalsIgnoreCase(type)) aepService.deleteAepReport(id, currentUser);
        else if ("risk".equalsIgnoreCase(type)) riskChecklistService.deleteReport(id, currentUser);
        else throw new IllegalArgumentException("Tipo de documento inválido: " + type);
    }

    /**
     * Recupera o PDF e gera um nome amigável para download.
     */
    @Transactional(readOnly = true)
    public FileDownloadDTO downloadDocument(String type, Long id, User currentUser) throws IOException {
        String pdfPathOnDisk = null;
        byte[] pdfBytes = null;

        // Variáveis para montar o nome
        String docTypeLabel = "";
        String title = "";
        String companyName = "";
        LocalDate date = LocalDate.now();

        if ("visit".equalsIgnoreCase(type)) {
            TechnicalVisit visit = technicalVisitRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Relatório não encontrado."));

            pdfPathOnDisk = visit.getPdfPath();
            docTypeLabel = "Visita Tecnica";
            title = visit.getTitle();
            companyName = visit.getClientCompany().getName();
            date = visit.getVisitDate();

        } else if ("aep".equalsIgnoreCase(type)) {
            AepReport aep = aepReportRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("AEP não encontrada."));

            // AEP pode gerar em tempo real se não tiver path, assumindo lógica similar ao seu service
            if (aep.getPdfPath() == null) {
                pdfBytes = aepService.loadOrGenerateAepPdf(id, currentUser);
            } else {
                pdfPathOnDisk = aep.getPdfPath();
            }

            docTypeLabel = "AEP";
            title = aep.getEvaluatedFunction(); // Ou outro campo de título
            companyName = aep.getCompany().getName();
            date = aep.getEvaluationDate();

        } else if ("risk".equalsIgnoreCase(type)) {
            OccupationalRiskReport report = riskReportRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Checklist não encontrado."));

            pdfPathOnDisk = report.getPdfPath();
            docTypeLabel = "Checklist Risco";
            title = report.getTitle();
            companyName = report.getCompany().getName();
            date = report.getInspectionDate();
        }

        // 1. Carrega os bytes (se já não foram gerados em memória para AEP)
        if (pdfBytes == null) {
            if (pdfPathOnDisk == null) throw new RuntimeException("Arquivo não encontrado no servidor.");
            Path path = ("visit".equalsIgnoreCase(type)) ? Paths.get(fileStoragePath, pdfPathOnDisk) : Paths.get(pdfPathOnDisk);
            pdfBytes = Files.readAllBytes(path);
        }

        // 2. Sanitiza e Monta o Nome do Arquivo
        // Formato: TIPO - TITULO - EMPRESA - DD-MM-YYYY.pdf
        String safeTitle = sanitizeFilename(title);
        String safeCompany = sanitizeFilename(companyName);
        String dateStr = date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        String finalFilename = String.format("%s - %s - %s - %s.pdf",
                docTypeLabel, safeTitle, safeCompany, dateStr);

        return new FileDownloadDTO(finalFilename, pdfBytes);
    }

    // Remove caracteres especiais que quebram o download
    private String sanitizeFilename(String input) {
        if (input == null) return "SemNome";
        // Mantém apenas letras, números, espaços, traços e underscores
        return input.replaceAll("[^a-zA-Z0-9 \\-_.]", "").trim();
    }

    // ===================================================================================
    // 5. HELPERS DE MAPEAMENTO (Com E-mail e Nome do Técnico)
    // ===================================================================================

    public DocumentSummaryDTO mapVisitToSummaryDto(TechnicalVisit visit) {
        DocumentSummaryDTO dto = new DocumentSummaryDTO();
        dto.setId(visit.getId());
        dto.setDocumentType("Relatório de Visita");
        dto.setTitle(visit.getTitle());
        dto.setCreationDate(visit.getVisitDate());
        dto.setPdfGenerated(visit.getPdfPath() != null && !visit.getPdfPath().isEmpty());
        dto.setIcpSigned(visit.getIcpSignedAt() != null);
        dto.setIcpSignedAt(visit.getIcpSignedAt());
        fillCommonFields(dto, visit.getClientCompany(), visit.getSentToClientAt(), visit.getTechnicianSignatureImageBase64(), visit.getTechnician());
        return dto;
    }

    public DocumentSummaryDTO mapAepToSummaryDto(AepReport aep) {
        DocumentSummaryDTO dto = new DocumentSummaryDTO();
        dto.setId(aep.getId());
        dto.setDocumentType("Avaliação Ergonômica Preliminar");
        dto.setTitle(aep.getEvaluatedFunction());
        dto.setCreationDate(aep.getEvaluationDate());
        dto.setPdfGenerated(aep.getPdfPath() != null && !aep.getPdfPath().isEmpty());
        dto.setIcpSigned(false);
        fillCommonFields(dto, aep.getCompany(), aep.getSentToClientAt(), null, aep.getEvaluator());
        return dto;
    }

    public DocumentSummaryDTO mapRiskToSummaryDto(OccupationalRiskReport report) {
        DocumentSummaryDTO dto = new DocumentSummaryDTO();
        dto.setId(report.getId());
        dto.setDocumentType("Checklist de Riscos");
        dto.setTitle(report.getTitle());
        dto.setCreationDate(report.getInspectionDate());
        dto.setPdfGenerated(report.getPdfPath() != null && !report.getPdfPath().isEmpty());
        dto.setIcpSigned(report.getIcpSignedAt() != null);
        dto.setIcpSignedAt(report.getIcpSignedAt());
        fillCommonFields(dto, report.getCompany(), report.getSentToClientAt(), report.getTechnicianSignatureImageBase64(), report.getTechnician());
        return dto;
    }

    private void fillCommonFields(DocumentSummaryDTO dto, Company company, LocalDateTime sentAt, String signatureBase64, User technician) {
        if (company != null) {
            dto.setClientName(company.getName());

            if (company.getClients() != null && !company.getClients().isEmpty()) {
                // Pega todos os e-mails dos clientes vinculados e junta com vírgula
                String emails = company.getClients().stream()
                        .map(Client::getEmail)
                        .collect(Collectors.joining(", "));
                dto.setClientEmail(emails);
            } else {
                dto.setClientEmail(null);
            }

        } else {
            dto.setClientName("N/A");
            dto.setClientEmail(null);
        }

        if (technician != null) {
            dto.setTechnicianName(technician.getName());
        }

        dto.setEmailSent(sentAt != null);
        dto.setSigned(signatureBase64 != null && !signatureBase64.isBlank());
    }

    /**
     * Exporta os documentos filtrados por data em um arquivo ZIP.
     * Escreve diretamente no fluxo de saída da resposta HTTP.
     */
    @Transactional(readOnly = true)
    public void exportDocumentsToZip(LocalDate startDate, LocalDate endDate, HttpServletResponse response) throws IOException {

        // 1. Configura os Headers da resposta para o download do ZIP
        response.setContentType("application/zip");
        String zipFilename = "backup_documentos_" + LocalDate.now() + ".zip";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + zipFilename + "\"");

        // 2. Busca todos os documentos globais (usando seu metodo existente)
        List<DocumentSummaryDTO> allDocs = fetchRawDocumentsGlobal();

        // 3. Aplica o filtro de data
        List<DocumentSummaryDTO> filteredDocs = allDocs.stream()
                .filter(doc -> {
                    if (doc.getCreationDate() == null) return false;
                    boolean afterStart = (startDate == null) || !doc.getCreationDate().isBefore(startDate);
                    boolean beforeEnd = (endDate == null) || !doc.getCreationDate().isAfter(endDate);
                    return afterStart && beforeEnd;
                })
                .toList();

        // 4. Cria o ZipOutputStream apontando direto para a saída da requisição HTTP
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {

            for (DocumentSummaryDTO doc : filteredDocs) {
                try {
                    // Mapeia o nome descritivo do tipo para a chave interna (visit, aep, risk)
                    String typeKey = mapDocumentTypeToKey(doc.getDocumentType());

                    // Reutiliza sua lógica maravilhosa de download para pegar bytes e nome sanitizado
                    // Nota: Passando null no usuário, pois assumimos permissão Admin neste endpoint
                    FileDownloadDTO fileInfo = downloadDocument(typeKey, doc.getId(), null);

                    // Cria a entrada no arquivo ZIP com o nome amigável gerado pelo seu serviço
                    ZipEntry zipEntry = new ZipEntry(fileInfo.getFilename());
                    zos.putNextEntry(zipEntry);

                    // Escreve os bytes do PDF no ZIP
                    zos.write(fileInfo.getData());
                    zos.closeEntry();

                } catch (Exception e) {
                    // Se um arquivo der erro (ex: PDF não gerado), loga e continua com os próximos
                    System.err.println("Erro ao adicionar documento ID " + doc.getId() + " ao ZIP: " + e.getMessage());
                }
            }
            // Finaliza o processo de zipagem
            zos.finish();
        }
    }

    // Helper para converter a string de exibição de volta para o padrão que o downloadDocument aceita
    private String mapDocumentTypeToKey(String documentType) {
        if ("Relatório de Visita".equalsIgnoreCase(documentType)) return "visit";
        if ("Avaliação Ergonômica Preliminar".equalsIgnoreCase(documentType)) return "aep";
        if ("Checklist de Riscos".equalsIgnoreCase(documentType)) return "risk";
        throw new IllegalArgumentException("Tipo de documento desconhecido para exportação: " + documentType);
    }

    /**
     * Lógica centralizada para envio de qualquer documento por e-mail aos clientes vinculados.
     */
    @Transactional
    public List<String> sendDocumentToClients(String type, Long id, User currentUser) throws IOException {

        // 1. Usa o seu metodo existente que já sabe buscar os bytes e gerar um nome limpo!
        // (Isso mata toda a repetição de código de sanitização e formatação de data)
        FileDownloadDTO fileInfo = downloadDocument(type, id, currentUser);

        Set<Client> clients;
        String companyName;
        String subjectType;

        // 2. Lógica de Negócio: Buscar clientes e atualizar status
        if ("risk".equalsIgnoreCase(type)) {
            OccupationalRiskReport report = riskReportRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Checklist não encontrado."));
            clients = report.getCompany().getClients();
            companyName = report.getCompany().getName();
            subjectType = "Checklist de Riscos";

            report.setSentToClientAt(LocalDateTime.now());
            riskReportRepository.save(report);

        } else if ("visit".equalsIgnoreCase(type)) {
            TechnicalVisit visit = technicalVisitRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Visita não encontrada."));
            clients = visit.getClientCompany().getClients();
            companyName = visit.getClientCompany().getName();
            subjectType = "Relatório de Visita Técnica";

            visit.setSentToClientAt(LocalDateTime.now());
            technicalVisitRepository.save(visit);

        } else if ("aep".equalsIgnoreCase(type)) {
            AepReport aep = aepReportRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("AEP não encontrada."));
            clients = aep.getCompany().getClients();
            companyName = aep.getCompany().getName();
            subjectType = "Avaliação Ergonômica (AEP)";

            aep.setSentToClientAt(LocalDateTime.now());
            aepReportRepository.save(aep);

        } else {
            throw new IllegalArgumentException("Tipo de documento inválido: " + type);
        }

        // 3. Validações de Negócio
        if (clients == null || clients.isEmpty()) {
            throw new IllegalStateException("A empresa deste relatório não possui clientes vinculados.");
        }

        List<String> validEmails = clients.stream()
                .map(Client::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .toList();

        if (validEmails.isEmpty()) {
            throw new IllegalStateException("Os clientes vinculados não possuem e-mail válido.");
        }

        // 4. Montagem e Envio do E-mail
        String subject = "Documento Emitido: " + subjectType + " - " + companyName;
        String body = buildEmailTemplate(subjectType, companyName);

        for (String email : validEmails) {
            try {
                // fileInfo.getFilename() já vem sanitizado pelo seu metodo downloadDocument!
                emailService.sendReportWithAttachment(email, subject, body, fileInfo.getData(), fileInfo.getFilename());
            } catch (Exception e) {
                System.err.println("Falha ao enviar e-mail para: " + email + ". Motivo: " + e.getMessage());
            }
        }

        return validEmails;
    }

    // Extração do HTML gigante para um metodo privado limpo
    private String buildEmailTemplate(String subjectType, String companyName) {
        return String.format(
                "<div style='font-family: \"Segoe UI\", Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; background-color: #ffffff; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;'>" +
                        "  <div style='background-color: #166534; padding: 24px; text-align: center;'>" +
                        "    <h2 style='color: #ffffff; margin: 0; font-weight: 600; font-size: 24px;'>Go-Tree Consultoria</h2>" +
                        "  </div>" +
                        "  <div style='padding: 32px 24px; color: #333333; line-height: 1.6;'>" +
                        "    <p style='font-size: 16px; margin-top: 0;'>Olá,</p>" +
                        "    <p style='font-size: 16px;'>Informamos que um novo documento técnico foi emitido e está disponível para sua análise.</p>" +
                        "    <div style='background-color: #f8f9fa; border-left: 4px solid #166534; padding: 16px; margin: 24px 0; border-radius: 4px;'>" +
                        "      <p style='margin: 4px 0;'><strong>📄 Documento:</strong> %s</p>" +
                        "      <p style='margin: 4px 0;'><strong>🏢 Empresa:</strong> %s</p>" +
                        "    </div>" +
                        "    <p style='font-size: 16px;'>O arquivo completo encontra-se em <strong>anexo (PDF)</strong> neste e-mail.</p>" +
                        "    <p style='margin-top: 32px;'>Estamos à disposição para quaisquer dúvidas.</p>" +
                        "    <p style='margin-bottom: 0;'>Atenciosamente,<br><strong>Equipe Go-Tree</strong></p>" +
                        "  </div>" +
                        "  <div style='background-color: #f4f4f4; padding: 16px; text-align: center; font-size: 12px; color: #666666; border-top: 1px solid #eeeeee;'>" +
                        "    <p style='margin: 4px 0;'>© Go-Tree Consultoria.</p>" +
                        "    <p style='margin: 4px 0;'>Este é um envio automático do nosso sistema.</p>" +
                        "  </div>" +
                        "</div>",
                subjectType, companyName
        );
    }
}