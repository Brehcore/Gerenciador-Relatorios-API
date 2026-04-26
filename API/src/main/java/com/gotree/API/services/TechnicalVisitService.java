package com.gotree.API.services;

import com.gotree.API.dto.visit.CreateTechnicalVisitRequestDTO;
import com.gotree.API.dto.visit.VisitFindingDTO;
import com.gotree.API.entities.AgendaEvent;
import com.gotree.API.entities.Company;
import com.gotree.API.entities.TechnicalVisit;
import com.gotree.API.entities.User;
import com.gotree.API.entities.VisitFinding;
import com.gotree.API.enums.Shift;
import com.gotree.API.repositories.AgendaEventRepository;
import com.gotree.API.repositories.CompanyRepository;
import com.gotree.API.repositories.SectorRepository;
import com.gotree.API.repositories.TechnicalVisitRepository;
import com.gotree.API.repositories.UnitRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

/**
 * Serviço responsável por gerenciar visitas técnicas, incluindo criação,
 * geração de relatórios PDF, e manipulação de imagens associadas.
 */
@Service
public class TechnicalVisitService {

    private final TechnicalVisitRepository technicalVisitRepository;
    private final CompanyRepository companyRepository;
    private final ReportService reportService;
    private final UnitRepository unitRepository;
    private final SectorRepository sectorRepository;
    private final AgendaEventRepository agendaEventRepository;
    private final DigitalSignatureService digitalSignatureService;

    @Value("${file.storage.path}")
    private String fileStoragePath;

    public TechnicalVisitService(TechnicalVisitRepository technicalVisitRepository, CompanyRepository companyRepository,
                                 ReportService reportService, UnitRepository unitRepository,
                                 SectorRepository sectorRepository, AgendaEventRepository agendaEventRepository,
                                 DigitalSignatureService digitalSignatureService) {
        this.technicalVisitRepository = technicalVisitRepository;
        this.companyRepository = companyRepository;
        this.reportService = reportService;
        this.unitRepository = unitRepository;
        this.sectorRepository = sectorRepository;
        this.agendaEventRepository = agendaEventRepository;
        this.digitalSignatureService = digitalSignatureService;
    }

    @Transactional
    public TechnicalVisit createAndGeneratePdf(CreateTechnicalVisitRequestDTO dto, User technician) {
        TechnicalVisit visit = new TechnicalVisit();
        visit.setTechnician(technician);

        // Delega o mapeamento pesado para o metodo auxiliar
        applyRequestDataToVisit(visit, dto);

        TechnicalVisit savedVisit = technicalVisitRepository.save(visit);

        createAgendaEventForNextVisit(savedVisit, dto.getNextVisitDate(), dto.getNextVisitShift(), dto.getEventHour(), technician);

        return generateAndSavePdf(savedVisit, dto.getNextVisitDate(), dto.getNextVisitShift());
    }

    @Transactional
    public void signExistingVisit(Long id, User signer) throws IOException {
        TechnicalVisit visit = technicalVisitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Visita Técnica não encontrada"));

        if (!visit.getTechnician().getId().equals(signer.getId())) {
            throw new SecurityException("Apenas o técnico responsável pode assinar digitalmente este documento.");
        }

        if (visit.getPdfPath() == null) {
            throw new IllegalStateException("O PDF ainda não foi gerado.");
        }

        Path path = Paths.get(fileStoragePath, visit.getPdfPath());
        if (!Files.exists(path)) {
            throw new IllegalStateException("Arquivo PDF não encontrado no disco.");
        }

        byte[] pdfBytes = Files.readAllBytes(path);
        byte[] signedBytes = digitalSignatureService.signPdf(pdfBytes, signer);

        Files.write(path, signedBytes);

        visit.setIcpSignedAt(LocalDateTime.now());
        technicalVisitRepository.save(visit);
    }

    @Transactional(readOnly = true)
    public List<TechnicalVisit> findAllByTechnician(User technician) {
        return technicalVisitRepository.findByTechnicianOrderByVisitDateDesc(technician);
    }

    @Transactional
    public void deleteVisit(Long visitId, User currentUser) {
        TechnicalVisit visit = technicalVisitRepository.findById(visitId)
                .orElseThrow(() -> new RuntimeException("Relatório de Visita com ID " + visitId + " não encontrado."));

        if (!visit.getTechnician().getId().equals(currentUser.getId())) {
            throw new SecurityException("Usuário não autorizado a deletar este relatório de visita.");
        }

        try {
            if (visit.getPdfPath() != null && !visit.getPdfPath().isBlank()) {
                Files.deleteIfExists(Paths.get(fileStoragePath, visit.getPdfPath()));
            }
        } catch (IOException e) {
            System.err.println("Falha ao deletar o arquivo PDF da visita: " + visit.getPdfPath());
        }

        technicalVisitRepository.deleteById(visitId);
    }

    /**
     * Endpoint exposto para o Controller validar a agenda
     */
    @Transactional(readOnly = true)
    public Map<String, Object> validateNextVisitSchedule(LocalDate nextDate, String nextShiftStr, User currentUser) {
        return internalValidateNextVisitSchedule(nextDate, nextShiftStr, currentUser);
    }


    // ===================================================================================
    // MÉTODOS PRIVADOS AUXILIARES (Para evitar duplicação de código)
    // ===================================================================================

    private void applyRequestDataToVisit(TechnicalVisit visit, CreateTechnicalVisitRequestDTO dto) {
        Company clientCompany = companyRepository.findById(dto.getClientCompanyId())
                .orElseThrow(() -> new RuntimeException("Empresa cliente com ID " + dto.getClientCompanyId() + " não encontrada."));
        visit.setClientCompany(clientCompany);

        // Chama o metodo auxiliar reaproveitável
        setCommonVisitFields(visit, dto.getUnitId(), dto.getSectorId(), dto.getTitle(),
                dto.getLocation(), dto.getSummary(), dto.getVisitDate(), dto.getStartTime());

        visit.setEndTime(LocalTime.now());

        applySignaturesToVisit(visit, dto.getTechnicianSignatureImageBase64(), dto.getClientSignatureImageBase64(),
                dto.getClientSignerName(), dto.getClientSignatureLatitude(), dto.getClientSignatureLongitude());

        visit.setTechnicianSignedAt(LocalDateTime.now());
        visit.setClientSignedAt(LocalDateTime.now());

        updateFindings(visit, dto.getFindings());
    }

    /**
     * Metodo interno sem anotação transacional para evitar alerta de Self-Invocation.
     */
    private Map<String, Object> internalValidateNextVisitSchedule(LocalDate nextDate, String nextShiftStr, User currentUser) {
        Map<String, Object> response = new HashMap<>();
        List<String> warnings = new ArrayList<>();

        response.put("blocked", false);
        response.put("blockMessage", null);
        response.put("warnings", warnings);

        if (nextDate == null) {
            return response;
        }

        try {
            // Validação 1: Limite global de visitas no dia (IGNORANDO CANCELADOS)
            long eventsInDay = agendaEventRepository.countByUserAndEventDateAndStatusNot(
                    currentUser, nextDate, com.gotree.API.enums.AgendaStatus.CANCELADO);

            if (eventsInDay >= 2) {
                response.put("blocked", true);
                response.put("blockMessage", "Você já possui 2 eventos agendados nesta data. Escolha outra data.");
                return response;
            }

            // Validação 2: Se enviou o turno, valida conflito específico do turno (IGNORANDO CANCELADOS)
            if (nextShiftStr != null && !nextShiftStr.isBlank()) {
                Shift shift = Shift.valueOf(nextShiftStr.toUpperCase());
                long eventConflict = agendaEventRepository.countByUserAndEventDateAndShiftAndStatusNot(
                        currentUser, nextDate, shift, com.gotree.API.enums.AgendaStatus.CANCELADO);

                if (eventConflict > 0) {
                    response.put("blocked", true);
                    response.put("blockMessage", "Você já possui um compromisso neste turno. Escolha outro turno ou data.");
                    return response;
                }
            }

            // ... (o restante do código a partir do List<AgendaEvent> otherEvents continua igualzinho)

            List<AgendaEvent> otherEvents = agendaEventRepository.findAllByEventDate(nextDate).stream()
                    .filter(event -> event.getStatus() != com.gotree.API.enums.AgendaStatus.CANCELADO)
                    .filter(event -> !event.getUser().getId().equals(currentUser.getId()))
                    .toList();

            for (AgendaEvent event : otherEvents) {
                String shiftName = event.getShift() != null ? event.getShift().name() : "A DEFINIR";
                warnings.add(event.getUser().getName() + " marcou visita neste dia (" + shiftName + ").");
            }

            response.put("warnings", warnings);
            return response;

        } catch (IllegalArgumentException e) {
            response.put("blocked", true);
            response.put("blockMessage", "Turno inválido fornecido.");
            return response;
        }
    }

    private void applySignaturesToVisit(TechnicalVisit visit, String techBase64, String clientBase64, String clientName, Double lat, Double lon) {
        if (techBase64 != null) visit.setTechnicianSignatureImageBase64(stripDataUrlPrefix(techBase64));
        if (clientBase64 != null) visit.setClientSignatureImageBase64(stripDataUrlPrefix(clientBase64));
        if (clientName != null) visit.setClientSignerName(clientName);
        if (lat != null) visit.setClientSignatureLatitude(lat);
        if (lon != null) visit.setClientSignatureLongitude(lon);
    }

    private void updateFindings(TechnicalVisit visit, List<VisitFindingDTO> findingsDto) {
        // 1. Remove os arquivos físicos antigos do VPS
        for (VisitFinding finding : visit.getFindings()) {
            try {
                if (finding.getPhotoPath1() != null) Files.deleteIfExists(Paths.get(finding.getPhotoPath1()));
                if (finding.getPhotoPath2() != null) Files.deleteIfExists(Paths.get(finding.getPhotoPath2()));
            } catch (IOException e) {
                System.err.println("Aviso: Falha ao remover foto de rascunho anterior.");
            }
        }

        // 2. Remove do banco
        visit.getFindings().clear();

        // 3. Adiciona os novos
        if (findingsDto != null && !findingsDto.isEmpty()) {
            findingsDto.forEach(findingDto -> {
                VisitFinding finding = mapFindingDtoToEntity(findingDto);
                finding.setTechnicalVisit(visit);
                visit.getFindings().add(finding);
            });
        }
    }

    private void createAgendaEventForNextVisit(TechnicalVisit visit, LocalDate nextDate, String nextShift, LocalTime eventHour,User currentUser) {
        if (nextDate == null) return;

        if (nextShift != null) {
            Map<String, Object> validation = internalValidateNextVisitSchedule(nextDate, nextShift, currentUser);
            if ((Boolean) validation.get("blocked")) {
                throw new IllegalStateException("BLOQUEIO DE AGENDA: " + validation.get("blockMessage"));
            }
        }

        AgendaEvent futureEvent = new AgendaEvent();
        futureEvent.setEventDate(nextDate);
        futureEvent.setEventHour(eventHour);

        if (nextShift != null && !nextShift.isBlank()) {
            try {
                futureEvent.setShift(Shift.valueOf(nextShift.toUpperCase()));
            } catch (IllegalArgumentException e) {
                System.err.println("Turno inválido recebido para agenda.");
            }
        }

        futureEvent.setUser(currentUser);
        futureEvent.setCompany(visit.getClientCompany());
        futureEvent.setUnit(visit.getUnit());
        futureEvent.setSector(visit.getSector());
        futureEvent.setTitle("Retorno: " + visit.getClientCompany().getName());
        futureEvent.setEventType(com.gotree.API.enums.AgendaEventType.VISITA_TECNICA);
        futureEvent.setStatus(com.gotree.API.enums.AgendaStatus.A_CONFIRMAR);
        futureEvent.setOriginTechnicalVisitId(visit.getId());

        agendaEventRepository.save(futureEvent);
    }

    private TechnicalVisit generateAndSavePdf(TechnicalVisit visit, LocalDate nextVisitDate, String nextVisitShift) {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("visit", visit);

        if (nextVisitDate != null) {
            templateData.put("nextVisitDateFormatted", nextVisitDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            templateData.put("nextVisitShiftLabel", nextVisitShift != null ? nextVisitShift : "-");
        } else {
            templateData.put("nextVisitDateFormatted", "A Definir");
            templateData.put("nextVisitShiftLabel", "-");
        }

        byte[] pdfBytes = reportService.generatePdfFromHtml("visit-report-template", templateData);

        try {
            String fileName = "technical_visit_" + visit.getId() + "_" + UUID.randomUUID() + ".pdf";
            Path path = Paths.get(fileStoragePath, fileName);
            Files.createDirectories(path.getParent());
            Files.write(path, pdfBytes);

            visit.setPdfPath(fileName);
            return technicalVisitRepository.save(visit);

        } catch (IOException e) {
            throw new RuntimeException("Falha ao salvar o PDF da visita: " + e.getMessage(), e);
        }
    }

    private VisitFinding mapFindingDtoToEntity(VisitFindingDTO dto) {
        VisitFinding finding = new VisitFinding();

        if (dto.getPhotoBase64_1() != null && !dto.getPhotoBase64_1().isEmpty()) {
            try {
                byte[] imageBytes = Base64.getMimeDecoder().decode(stripDataUrlPrefix(dto.getPhotoBase64_1()));
                String imageFileName = "finding_" + UUID.randomUUID() + ".jpg";
                Path imagePath = Paths.get(fileStoragePath, "visit_photos", imageFileName);
                Files.createDirectories(imagePath.getParent());
                Files.write(imagePath, imageBytes);
                finding.setPhotoPath1(imagePath.toAbsolutePath().toString().replace("\\", "/"));
            } catch (IOException e) {
                throw new RuntimeException("Erro ao processar a imagem 1 do achado.", e);
            }
        }

        if (dto.getPhotoBase64_2() != null && !dto.getPhotoBase64_2().isEmpty()) {
            try {
                byte[] imageBytes2 = Base64.getMimeDecoder().decode(stripDataUrlPrefix(dto.getPhotoBase64_2()));
                String imageFileName = "finding_" + UUID.randomUUID() + ".jpg";
                Path imagePath = Paths.get(fileStoragePath, "visit_photos", imageFileName);
                Files.createDirectories(imagePath.getParent());
                Files.write(imagePath, imageBytes2);
                finding.setPhotoPath2(imagePath.toAbsolutePath().toString().replace("\\", "/"));
            } catch (IOException e) {
                throw new RuntimeException("Erro ao processar a imagem 2 do achado.", e);
            }
        }

        finding.setDescription(dto.getDescription());
        finding.setConsequences(dto.getConsequences());
        finding.setLegalGuidance(dto.getLegalGuidance());
        finding.setResponsible(dto.getResponsible());
        finding.setPenalties(dto.getPenalties());
        finding.setDeadline(dto.getDeadline());
        finding.setRecurrence(dto.isRecurrence());

        if (dto.getPriority() != null && !dto.getPriority().isBlank()) {
            try {
                finding.setPriority(VisitFinding.Priority.valueOf(dto.getPriority().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Prioridade inválida: " + dto.getPriority());
            }
        }

        return finding;
    }

    private String stripDataUrlPrefix(String dataUrl) {
        if (dataUrl == null) return null;
        int commaIndex = dataUrl.indexOf(',');
        return commaIndex != -1 ? dataUrl.substring(commaIndex + 1) : dataUrl;
    }

    private void setCommonVisitFields(TechnicalVisit visit, Long unitId, Long sectorId,
                                      String title, String location, String summary,
                                      LocalDate visitDate, LocalTime startTime) {

        visit.setUnit(unitId != null ? unitRepository.findById(unitId).orElse(null) : null);
        visit.setSector(sectorId != null ? sectorRepository.findById(sectorId).orElse(null) : null);
        visit.setTitle(title);
        visit.setLocation(location);
        visit.setSummary(summary);
        visit.setVisitDate(visitDate);
        visit.setStartTime(startTime);
    }
}