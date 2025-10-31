package com.gotree.API.services;

import com.gotree.API.dto.report.AepRequestDTO;
import com.gotree.API.entities.*;
import com.gotree.API.repositories.AepReportRepository;
import com.gotree.API.repositories.CompanyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AepService {

    // A LISTA MESTRE DE TODOS OS RISCOS
    private static final List<String> MASTER_RISK_LIST = Arrays.asList(
            "Trabalho em posturas incômodas ou pouco confortáveis por longos periodos",
            "Postura sentada por longos períodos",
            "Postura de pé por longos periodos",
            "Frequente deslocamento a pé durante a jornada de trabalho",
            // ... (todos os outros riscos da sua lista) ...
            "Piso escorregadio e/ou irregular"
    );

    private final AepReportRepository aepReportRepository;
    private final CompanyRepository companyRepository;
    private final ReportService reportService;

    @Value("${file.storage.path}")
    private String fileStoragePath;

    @Value("${app.generating-company.name}")
    private String generatingCompanyName;
    @Value("${app.generating-company.cnpj}")
    private String generatingCompanyCnpj;

    public AepService(AepReportRepository aepReportRepository, CompanyRepository companyRepository,
                      ReportService reportService) {
        this.aepReportRepository = aepReportRepository;
        this.companyRepository = companyRepository;
        this.reportService = reportService;
    }

    // apenas cria ou atualiza os dados no banco
    @Transactional
    public AepReport saveAepData(AepRequestDTO dto, User evaluator, Long existingId) {
        // Se um ID foi fornecido, busca o relatório para edição
        AepReport aep = (existingId != null)
                ? aepReportRepository.findById(existingId).orElseThrow(() -> new RuntimeException("AEP não encontrada"))
                : new AepReport();

        // Validação de segurança (só o criador ou admin pode editar)
        if (existingId != null && !aep.getEvaluator().getId().equals(evaluator.getId())) {
            // Você pode adicionar uma verificação de "ROLE_ADMIN" aqui
            throw new SecurityException("Usuário não autorizado a editar esta AEP.");
        }

        Company company = companyRepository.findById(dto.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada."));

        aep.setCompany(company);
        aep.setEvaluator(evaluator);
        aep.setEvaluationDate(dto.getEvaluationDate());
        aep.setEvaluatedFunction(dto.getEvaluatedFunction());
        aep.setSelectedRisks(dto.getSelectedRiskIds());

        // Dados da Fisio
        aep.setPhysioName(dto.getPhysioName());
        aep.setPhysioCrefito(dto.getPhysioCrefito());

        // Se o documento foi editado, limpa o caminho do PDF antigo
        if (existingId != null && aep.getPdfPath() != null) {
            deletePdfFile(aep.getPdfPath()); // Deleta o arquivo físico
            aep.setPdfPath(null); // Limpa o caminho no banco
        }

        return aepReportRepository.save(aep);
    }

    // Carrega um PDF existente ou gera um novo sob demanda
    @Transactional
    public byte[] loadOrGenerateAepPdf(Long id, User currentUser) throws IOException {
        AepReport aep = aepReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AEP com ID " + id + " não encontrada."));

        // Se o PDF já foi gerado e salvo, apenas o retorna
        if (aep.getPdfPath() != null && !aep.getPdfPath().isBlank()) {
            try {
                Path path = Paths.get(aep.getPdfPath());
                if (Files.exists(path)) {
                    return Files.readAllBytes(path);
                }
            } catch (Exception e) {
                // Se o arquivo não existir (ex: foi apagado do disco), geramos um novo
            }
        }

        // Se o PDF não existe (novo ou editado), GERA UM NOVO
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("aep", aep);
        templateData.put("company", aep.getCompany());
        templateData.put("evaluator", aep.getEvaluator());
        templateData.put("generatingCompanyName", generatingCompanyName);
        templateData.put("generatingCompanyCnpj", generatingCompanyCnpj); // Passa o CNPJ da Go-Tree
        templateData.put("allRisks", MASTER_RISK_LIST);
        templateData.put("selectedRisks", aep.getSelectedRisks());

        byte[] pdfBytes = reportService.generatePdfFromHtml("aep-template", templateData);

        // Salva o novo PDF no disco e atualiza a entidade
        String fileName = "AEP_" + aep.getId() + "_" + UUID.randomUUID() + ".pdf";
        Path path = Paths.get(fileStoragePath, fileName);
        Files.createDirectories(path.getParent());
        Files.write(path, pdfBytes);

        aep.setPdfPath(path.toString()); // Salva o caminho do NOVO PDF
        aepReportRepository.save(aep);

        return pdfBytes;
    }

    // Deleta o relatório e o arquivo PDF associado
    @Transactional
    public void deleteAepReport(Long id, User currentUser) {
        AepReport aep = aepReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AEP com ID " + id + " não encontrada."));

        if (!aep.getEvaluator().getId().equals(currentUser.getId())) {
            // Adicionar verificação de ROLE_ADMIN se necessário
            throw new SecurityException("Usuário não autorizado a deletar esta AEP.");
        }

        if (aep.getPdfPath() != null) {
            deletePdfFile(aep.getPdfPath());
        }

        aepReportRepository.delete(aep);
    }

    private void deletePdfFile(String pdfPath) {
        try {
            Path path = Paths.get(pdfPath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            System.err.println("Falha ao deletar arquivo PDF antigo: " + pdfPath);
        }
    }
}