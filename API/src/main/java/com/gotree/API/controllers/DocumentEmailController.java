package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;

import com.gotree.API.entities.User;
import com.gotree.API.services.DocumentAggregationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller responsável por gerenciar o envio de documentos por e-mail.
 * Fornece endpoints para enviar diferentes tipos de relatórios (Risco Ocupacional, Visita Técnica, AEP)
 * para clientes cadastrados no sistema.
 */
@Tag(name = "Envio de Documentos por E-mail", description = "Envio de documentos por e-mail aos clientes.")
@RestController
@RequestMapping("/documents/email")
public class DocumentEmailController {

    private final DocumentAggregationService documentService;
    private static final Logger log = LoggerFactory.getLogger(DocumentEmailController.class);

    /**
     * Construtor para DocumentEmailController.
     *
     * @param documentService o serviço responsável pelas operações de agregação de documentos e envio de e-mail
     */
    public DocumentEmailController(DocumentAggregationService documentService) {
        this.documentService = documentService;
    }

    /**
     * Envia um documento por e-mail para os clientes associados.
     * Apenas usuários autenticados podem acessar este endpoint.
     *
     * @param type o tipo de documento a ser enviado (ex: risco ocupacional, visita técnica, AEP)
     * @param id   o identificador do documento a ser enviado
     * @param auth o objeto de autenticação contendo os detalhes do usuário atual
     * @return ResponseEntity com mensagem de sucesso e lista de e-mails se o envio for concluído com sucesso,
     * ou mensagem de erro se uma exceção ocorrer durante o processo
     */
    @Operation(summary = "Envia um documento por e-mail", description = "Envia um documento PDF para os clientes vinculados.")
    @PostMapping("/{type}/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> sendDocumentToClient(@PathVariable String type, @PathVariable Long id, Authentication auth) {
        User user = ((CustomUserDetails) auth.getPrincipal()).user();

        try {
            List<String> validEmails = documentService.sendDocumentToClients(type, id, user);

            String allEmails = String.join(", ", validEmails);
            return ResponseEntity.ok().body(Map.of(
                    "message", "Documento enviado com sucesso.",
                    "emails_enviados", allEmails
            ));

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            log.error("Erro interno ao enviar e-mail: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ocorreu um erro interno ao processar sua solicitação."));
        }
    }
}