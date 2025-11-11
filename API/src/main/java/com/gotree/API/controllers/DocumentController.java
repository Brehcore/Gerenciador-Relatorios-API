package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.document.DocumentSummaryDTO;
import com.gotree.API.entities.User;
import com.gotree.API.services.DocumentAggregationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * Controlador REST responsável por gerenciar documentos relacionados a visitas técnicas.
 * Fornece endpoints para listar, baixar e excluir documentos gerados durante as visitas.
 * Todos os endpoints requerem autenticação do usuário.
 */
@RestController
@RequestMapping("/documents")
public class DocumentController {
    
    

    private final DocumentAggregationService documentAggregationService;

    public DocumentController(DocumentAggregationService documentAggregationService) {
        this.documentAggregationService = documentAggregationService;
    }

    /**
     * Retorna todos os documentos associados ao usuário autenticado.
     * Este endpoint é utilizado na página "Gerenciar Documentos" para listar
     * documentos de todos os tipos de visitas.
     *
     * @param authentication Objeto de autenticação do Spring Security contendo os detalhes do usuário
     * @return ResponseEntity com uma lista de DocumentSummaryDTO contendo os resumos dos documentos
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DocumentSummaryDTO>> getAllMyDocuments(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User technician = userDetails.getUser();

        List<DocumentSummaryDTO> allDocuments = documentAggregationService.findAllDocumentsForUser(technician);

        return ResponseEntity.ok(allDocuments);
    }

    /**
     * Retorna os documentos mais recentes do usuário autenticado.
     * Este endpoint é utilizado no dashboard para exibir um histórico resumido
     * dos últimos documentos gerados, limitado aos 5 mais recentes.
     *
     * @param authentication Objeto de autenticação do Spring Security contendo os detalhes do usuário
     * @return ResponseEntity com uma lista limitada de DocumentSummaryDTO
     */
    @GetMapping("/latest")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DocumentSummaryDTO>> getMyLatestDocuments(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User technician = userDetails.getUser();

        List<DocumentSummaryDTO> latestDocuments = documentAggregationService.findLatestDocumentsForUser(technician);

        return ResponseEntity.ok(latestDocuments);
    }

    /**
     * Permite o download ou visualização de um documento PDF específico.
     * O documento é identificado pelo seu tipo (ex: visita, inspeção) e ID.
     * Verifica se o usuário tem permissão para acessar o documento solicitado.
     *
     * @param type Tipo do documento (ex: "visit", "inspection")
     * @param id ID do documento
     * @param authentication Objeto de autenticação do Spring Security
     * @return ResponseEntity contendo o arquivo PDF ou status de erro apropriado
     * @throws IOException Em caso de erro ao ler o arquivo
     */
    @GetMapping("/{type}/{id}/pdf") // Ex: /documents/visit/45/pdf
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadDocumentPdf(@PathVariable String type, @PathVariable Long id, Authentication authentication) {


        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User currentUser = userDetails.getUser();

            // Agora passamos o tipo e o ID para o serviço
            byte[] pdfBytes = documentAggregationService.loadPdfFileByTypeAndId(type, id, currentUser);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = type + "_" + id + ".pdf";
            headers.setContentDispositionFormData("attachment", filename);


            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (RuntimeException e) {

            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{type}/{id}") // Ex: DELETE /documents/visit/45
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteDocument(@PathVariable String type, @PathVariable Long id, Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User currentUser = userDetails.getUser();

            // Agora passamos o tipo e o ID para o serviço
            documentAggregationService.deleteDocumentByTypeAndId(type, id, currentUser);

            return ResponseEntity.noContent().build(); // Retorna 204 No Content (sucesso)

        } catch (SecurityException e) {
            // Captura o erro se o usuário não for o dono do documento
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (RuntimeException e) {
            // Captura o erro se o documento não for encontrado ou o tipo for inválido
            return ResponseEntity.notFound().build();
        }
    }
}