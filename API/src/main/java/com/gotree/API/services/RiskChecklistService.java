package com.gotree.API.services;

import com.gotree.API.dto.risk.EvaluatedFunctionRequestDTO;
import com.gotree.API.dto.risk.SaveRiskReportRequestDTO;
import com.gotree.API.entities.*;
import com.gotree.API.repositories.*;
import com.gotree.API.utils.RiskCatalog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Serviço responsável pela gestão de checklists de riscos ocupacionais.
 * Gerencia a criação, geração de PDFs e exclusão de relatórios de riscos ocupacionais.
 */
@Service
public class RiskChecklistService {


    /**
     * Repositório para operações com relatórios de riscos ocupacionais
     */
    private final OccupationalRiskReportRepository reportRepository;
    /**
     * Repositório para operações com empresas
     */
    private final CompanyRepository companyRepository;
    /**
     * Repositório para operações com unidades
     */
    private final UnitRepository unitRepository;
    /**
     * Repositório para operações com setores
     */
    private final SectorRepository sectorRepository;
    /**
     * Serviço para geração de relatórios
     */
    private final ReportService reportService;
    /**
     * Repositório para informações do sistema
     */
    private final SystemInfoRepository systemInfoRepository;

    /**
     * Caminho para armazenamento de arquivos
     */
    @Value("${file.storage.path}")
    private String fileStoragePath;

//    @Value("${app.generating-company.name}")
//    private String generatingCompanyName;
//    @Value("${app.generating-company.cnpj}")
//    private String generatingCompanyCnpj;

    /**
     * Construtor que inicializa os repositórios e serviços necessários.
     *
     * @param reportRepository     Repositório de relatórios de riscos ocupacionais
     * @param companyRepository    Repositório de empresas
     * @param unitRepository       Repositório de unidades
     * @param sectorRepository     Repositório de setores
     * @param reportService        Serviço de geração de relatórios
     * @param systemInfoRepository Repositório de informações do sistema
     */
    public RiskChecklistService(OccupationalRiskReportRepository reportRepository,
                                CompanyRepository companyRepository,
                                UnitRepository unitRepository,
                                SectorRepository sectorRepository,
                                ReportService reportService,
                                SystemInfoRepository systemInfoRepository) {
        this.reportRepository = reportRepository;
        this.companyRepository = companyRepository;
        this.unitRepository = unitRepository;
        this.sectorRepository = sectorRepository;
        this.reportService = reportService;
        this.systemInfoRepository = systemInfoRepository;
    }

    /**
     * Cria um novo relatório de riscos ocupacionais e gera seu PDF correspondente.
     *
     * @param dto        DTO contendo os dados necessários para criar o relatório
     * @param technician Técnico responsável pelo relatório
     * @return Relatório criado com o caminho do PDF gerado
     * @throws RuntimeException se a empresa não for encontrada ou houver erro na geração do PDF
     */
    @Transactional
    public OccupationalRiskReport createAndGeneratePdf(SaveRiskReportRequestDTO dto, User technician) {
        // 1. Buscando Entidades Relacionadas
        Company company = companyRepository.findById(dto.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));
        Unit unit = dto.getUnitId() != null ? unitRepository.findById(dto.getUnitId()).orElse(null) : null;
        Sector sector = dto.getSectorId() != null ? sectorRepository.findById(dto.getSectorId()).orElse(null) : null;

        // 2. Criando o Relatório
        OccupationalRiskReport report = new OccupationalRiskReport();
        report.setCompany(company);
        report.setUnit(unit);
        report.setSector(sector);
        report.setTechnician(technician);
        report.setInspectionDate(dto.getInspectionDate());

        // Assinatura (Opcional)
        if (dto.getTechnicianSignatureImageBase64() != null && !dto.getTechnicianSignatureImageBase64().isBlank()) {
            report.setTechnicianSignatureImageBase64(stripDataUrlPrefix(dto.getTechnicianSignatureImageBase64()));
            report.setTechnicianSignedAt(LocalDateTime.now());
        } else {
            // Se não enviou assinatura, deixa nulo
            report.setTechnicianSignatureImageBase64(null);
            report.setTechnicianSignedAt(null);
        }

        // 3. Mapeando Funções e Riscos
        if (dto.getFunctions() != null) {
            for (EvaluatedFunctionRequestDTO funcDto : dto.getFunctions()) {
                EvaluatedFunction evalFunc = new EvaluatedFunction();
                evalFunc.setFunctionName(funcDto.getFunctionName());
                evalFunc.setSelectedRiskCodes(funcDto.getSelectedRiskCodes()); // Salva apenas os IDs (Integer)
                evalFunc.setReport(report);

                report.getEvaluatedFunctions().add(evalFunc);
            }
        }

        // 4. Salva no banco (gera ID)
        OccupationalRiskReport savedReport = reportRepository.save(report);

        // 5. Gera o PDF
        return generatePdf(savedReport);
    }

    /**
     * Gera um arquivo PDF para o relatório especificado.
     *
     * @param report Relatório para o qual o PDF será gerado
     * @return Relatório atualizado com o caminho do PDF
     * @throws RuntimeException se houver erro na geração ou salvamento do PDF
     */
    private OccupationalRiskReport generatePdf(OccupationalRiskReport report) {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("report", report);
        SystemInfo myInfo = systemInfoRepository.findFirst();

        if (myInfo != null) {
            templateData.put("generatingCompanyName", myInfo.getCompanyName());
            templateData.put("generatingCompanyCnpj", myInfo.getCnpj());
            templateData.put("generatingCompanyLogo", myInfo.getLogoBase64());
        } else {
            // Fallback caso o banco esteja vazio (opcional)
            templateData.put("generatingCompanyName", "Go-Tree Consultoria (Configurar DB)");
            templateData.put("generatingCompanyCnpj", "");
            templateData.put("generatingCompanyLogo", null);
        }

        List<Map<String, Object>> functionsData = new ArrayList<>();

        for (EvaluatedFunction func : report.getEvaluatedFunctions()) {
            Map<String, Object> funcMap = new HashMap<>();
            funcMap.put("name", func.getFunctionName());

            // Converte lista de IDs (Integer) em lista de Objetos (RiskItem)
            List<RiskCatalog.RiskItem> risks = new ArrayList<>();
            if (func.getSelectedRiskCodes() != null) {
                for (Integer code : func.getSelectedRiskCodes()) {
                    RiskCatalog.RiskItem item = RiskCatalog.getByCode(code);
                    if (item != null) {
                        risks.add(item);
                    }
                }
            }
            funcMap.put("risks", risks); // Lista de objetos com .type e .factor
            functionsData.add(funcMap);
        }

        templateData.put("functionsData", functionsData);
        // --------------------------------------------

        byte[] pdfBytes = reportService.generatePdfFromHtml("risk-checklist-template", templateData);

        try {
            String fileName = "RISK_CHECKLIST_" + report.getId() + "_" + UUID.randomUUID() + ".pdf";
            Path path = Paths.get(fileStoragePath, fileName);
            Files.createDirectories(path.getParent());
            Files.write(path, pdfBytes);

            report.setPdfPath(path.toString()); // Salva caminho completo
            return reportRepository.save(report);

        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar PDF: " + e.getMessage());
        }
    }

    /**
     * Exclui um relatório e seu arquivo PDF associado.
     *
     * @param id          ID do relatório a ser excluído
     * @param currentUser Usuário que está solicitando a exclusão
     * @throws RuntimeException  se o relatório não for encontrado
     * @throws SecurityException se o usuário não tiver permissão para excluir
     */
    @Transactional
    public void deleteReport(Long id, User currentUser) {
        OccupationalRiskReport report = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Relatório não encontrado"));

        if (!report.getTechnician().getId().equals(currentUser.getId())) {
            throw new SecurityException("Sem permissão para deletar.");
        }

        try {
            if (report.getPdfPath() != null) {
                Files.deleteIfExists(Paths.get(report.getPdfPath()));
            }
        } catch (IOException e) {
            System.err.println("Erro ao deletar arquivo: " + e.getMessage());
        }

        reportRepository.delete(report);
    }

    /**
     * Remove o prefixo de Data URL de uma string base64.
     *
     * @param dataUrl String contendo a URL de dados
     * @return String base64 sem o prefixo ou null se a entrada for null
     */
    private String stripDataUrlPrefix(String dataUrl) {
        if (dataUrl == null) return null;
        int commaIndex = dataUrl.indexOf(',');
        return commaIndex != -1 ? dataUrl.substring(commaIndex + 1) : dataUrl;
    }
}