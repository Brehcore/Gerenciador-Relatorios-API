package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.entities.AepReport;
import com.gotree.API.entities.OccupationalRiskReport;
import com.gotree.API.entities.TechnicalVisit;
import com.gotree.API.entities.User;
import com.gotree.API.repositories.AepReportRepository;
import com.gotree.API.repositories.OccupationalRiskReportRepository;
import com.gotree.API.repositories.TechnicalVisitRepository;
import com.gotree.API.services.DocumentAggregationService;
import com.gotree.API.services.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controlador REST responsável por gerenciar o envio de documentos por e-mail.
 * Oferece endpoints para enviar diferentes tipos de relatórios (Risco Ocupacional, Visita Técnica, AEP)
 * aos clientes cadastrados no sistema.
 */
@RestController
@RequestMapping("/documents/email")
public class DocumentEmailController {

    private final DocumentAggregationService documentService;
    private final EmailService emailService;
    private final OccupationalRiskReportRepository riskRepo;
    private final AepReportRepository aepRepo;
    private final TechnicalVisitRepository visitRepo;

    /**
     * Construtor da classe DocumentEmailController.
     *
     * @param documentService Serviço para agregação e geração de documentos
     * @param emailService    Serviço para envio de e-mails
     * @param riskRepo        Repositório de relatórios de risco ocupacional
     * @param aepRepo         Repositório de relatórios AEP
     * @param visitRepo       Repositório de visitas técnicas
     */
    public DocumentEmailController(DocumentAggregationService documentService,
                                   EmailService emailService,
                                   OccupationalRiskReportRepository riskRepo,
                                   AepReportRepository aepRepo,
                                   TechnicalVisitRepository visitRepo) {
        this.documentService = documentService;
        this.emailService = emailService;
        this.riskRepo = riskRepo;
        this.aepRepo = aepRepo;
        this.visitRepo = visitRepo;
    }

    /**
     * Gera o PDF do documento solicitado, encontra o e-mail do cliente vinculado
     * e envia o arquivo como anexo. Atualiza o status de envio no banco.
     *
     * @param type Tipo do documento (risk, visit, aep)
     * @param id ID do documento
     * @param auth Usuário autenticado (necessário para gerar o PDF)
     */
    @PostMapping("/{type}/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> sendDocumentToClient(@PathVariable String type, @PathVariable Long id, Authentication auth) {
        User user = ((CustomUserDetails) auth.getPrincipal()).user();

        try {
            // 1. Gera/Carrega os bytes do PDF usando a lógica centralizada
            // Isso garante que o PDF enviado é o mesmo que o usuário baixa na tela
            byte[] pdfBytes = documentService.loadPdfFileByTypeAndId(type, id, user);

            String clientEmail = null;
            String companyName = "";
            String docName = type.toUpperCase() + "_" + id + ".pdf";
            String subjectType = "";

            // 2. Lógica de Seleção baseada no Tipo
            // Busca o relatório, valida se tem cliente e atualiza o status 'sentToClientAt'

            if ("risk".equalsIgnoreCase(type)) {
                OccupationalRiskReport report = riskRepo.findById(id)
                        .orElseThrow(() -> new RuntimeException("Checklist de Risco não encontrado."));

                if (report.getCompany().getClient() == null) {
                    return ResponseEntity.badRequest().body(Map.of("error", "A empresa deste relatório não possui um Cliente vinculado."));
                }

                clientEmail = report.getCompany().getClient().getEmail();
                companyName = report.getCompany().getName();
                subjectType = "Checklist de Riscos";

                // Marca como enviado (Ícone ficará verde)
                report.setSentToClientAt(LocalDateTime.now());
                riskRepo.save(report);

            } else if ("visit".equalsIgnoreCase(type)) {
                TechnicalVisit visit = visitRepo.findById(id)
                        .orElseThrow(() -> new RuntimeException("Relatório de Visita não encontrado."));

                if (visit.getClientCompany().getClient() == null) {
                    return ResponseEntity.badRequest().body(Map.of("error", "A empresa deste relatório não possui um Cliente vinculado."));
                }

                clientEmail = visit.getClientCompany().getClient().getEmail();
                companyName = visit.getClientCompany().getName();
                subjectType = "Relatório de Visita Técnica";

                visit.setSentToClientAt(LocalDateTime.now());
                visitRepo.save(visit);

            } else if ("aep".equalsIgnoreCase(type)) {
                AepReport aep = aepRepo.findById(id)
                        .orElseThrow(() -> new RuntimeException("AEP não encontrada."));

                if (aep.getCompany().getClient() == null) {
                    return ResponseEntity.badRequest().body(Map.of("error", "A empresa deste relatório não possui um Cliente vinculado."));
                }

                clientEmail = aep.getCompany().getClient().getEmail();
                companyName = aep.getCompany().getName();
                subjectType = "Avaliação Ergonômica (AEP)";

                aep.setSentToClientAt(LocalDateTime.now());
                aepRepo.save(aep);

            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Tipo de documento inválido: " + type));
            }

            // 3. Validação final do e-mail
            if (clientEmail == null || clientEmail.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "O cadastro do cliente não possui um e-mail válido."));
            }

            // 4. Construção do E-mail
            String subject = "Documento Emitido: " + subjectType + " - " + companyName;

            String body = String.format(
                    "<div style='font-family: Arial, sans-serif; color: #333;'>" +
                            "  <h2>Olá!</h2>" +
                            "  <p>Segue em anexo o documento <strong>%s</strong> referente à empresa <strong>%s</strong>.</p>" +
                            "  <p>Este e-mail foi enviado automaticamente pela Go-Tree Consultoria.</p>" +
                            "  <br>" +
                            "  <p style='font-size: 12px; color: #777;'>Favor não responder este e-mail.</p>" +
                            "</div>",
                    subjectType, companyName
            );

            // 5. Envio
            emailService.sendReportWithAttachment(clientEmail, subject, body, pdfBytes, docName);

            return ResponseEntity.ok().body(Map.of(
                    "message", "E-mail enviado com sucesso para " + clientEmail,
                    "email", clientEmail
            ));

        } catch (Exception e) {
            e.printStackTrace(); // Útil para debug no console
            return ResponseEntity.internalServerError().body(Map.of("error", "Erro ao processar envio: " + e.getMessage()));
        }
    }
}