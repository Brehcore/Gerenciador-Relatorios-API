package com.gotree.API.controllers;

import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.document.DocumentSummaryDTO;
import com.gotree.API.dto.document.FileDownloadDTO;
import com.gotree.API.entities.User;
import com.gotree.API.services.DocumentAggregationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Controlador REST responsável por gerenciar documentos.
 * Fornece endpoints para listar, baixar e excluir documentos gerados.
 * Todos os endpoints requerem autenticação do usuário.
 */
@Tag(name = "Gerenciar Documentos", description = "Responsável por gerenciar documentos")
@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentAggregationService documentAggregationService;
    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    public DocumentController(DocumentAggregationService documentAggregationService) {
        this.documentAggregationService = documentAggregationService;
    }

    /**
     * Retorna todos os documentos associados ao usuário autenticado, com filtros e paginação.
     *
     * @param authentication Objeto de autenticação
     * @param type Filtro por Tipo (visit, aep, risk)
     * @param clientName Filtro por nome do Cliente
     * @param startDate Filtro de data inicial (formato AAAA-MM-DD)
     * @param endDate Filtro de data final (formato AAAA-MM-DD)
     * @param pageable Objeto do Spring que contém a página (page=) e o tamanho (size=)
     * @return ResponseEntity com uma Página (Page) de DocumentSummaryDTO
     */
    @Operation(summary = "Retorna todos os documentos", description = "Retorna todos os documentos associados ao usuário autenticado, com filtro e paginação")
    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_REPORTS') or hasRole('ADMIN')")
    public ResponseEntity<Page<DocumentSummaryDTO>> getAllMyDocuments(
            Authentication authentication,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String clientName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User technician = userDetails.user();

        // Passa os novos filtros e a paginação para o serviço
        Page<DocumentSummaryDTO> documentsPage = documentAggregationService.findAllDocumentsForUser(
                technician, type, clientName, startDate, endDate, pageable
        );

        return ResponseEntity.ok(documentsPage);
    }

    /**
     * Retorna os documentos mais recentes do usuário autenticado.
     * Este endpoint é utilizado no dashboard para exibir um histórico resumido
     * dos últimos documentos gerados, limitado aos 5 mais recentes.
     *
     * @param authentication Objeto de autenticação do Spring Security contendo os detalhes do usuário
     * @return ResponseEntity com uma lista limitada de DocumentSummaryDTO
     */
    @Operation(summary = "Retorna documentos mais recentes", description = "Retorna os documentos mais recentes do usuário autenticado.")
    @GetMapping("/latest")
    @PreAuthorize("hasAuthority('VIEW_REPORTS') or hasRole('ADMIN')")
    public ResponseEntity<List<DocumentSummaryDTO>> getMyLatestDocuments(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User technician = userDetails.user();

        List<DocumentSummaryDTO> latestDocuments = documentAggregationService.findLatestDocumentsForUser(technician);

        return ResponseEntity.ok(latestDocuments);
    }

    /**
     * Permite o download ou visualização de um documento PDF específico.
     * O documento é identificado pelo seu tipo (ex: visita, inspeção) e ID.
     * Verifica se o usuário tem permissão para acessar o documento solicitado.
     * Agora utiliza FileDownloadDTO para garantir o nome correto do arquivo.
     * @param type Tipo do documento (ex: "visit", "inspection")
     * @param id ID do documento
     * @param authentication Objeto de autenticação do Spring Security
     * @return ResponseEntity contendo o arquivo PDF ou status de erro apropriado
     */
    @Operation(summary = "Download ou visualização do documento", description = "Permite o download ou visualização de um documento PDF específico.")
    @GetMapping("/{type}/{id}/pdf")
    @PreAuthorize("hasAuthority('VIEW_REPORTS') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadDocumentPdf(@PathVariable String type, @PathVariable Long id, Authentication authentication) {

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User currentUser = userDetails.user();

            // 1. Chama o novo metodo que retorna o DTO (Nome + Bytes)
            FileDownloadDTO fileDto = documentAggregationService.downloadDocument(type, id, currentUser);

            // 2. Retorna com o cabeçalho Content-Disposition configurado com o nome correto
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileDto.getFilename() + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(fileDto.getData());

        } catch (IOException e) {
            logger.error("Falha ao ler o arquivo PDF do disco. Tipo: {}, ID: {}. Erro: {}", type, id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Exclui um documento específico com base no tipo e ID fornecidos.
     * Verifica se o usuário autenticado tem permissão para excluir o documento solicitado.
     * Apenas documentos pertencentes ao usuário podem ser excluídos.
     *
     * @param type           Tipo do documento a ser excluído (ex: "visit", "inspection")
     * @param id             ID único do documento a ser excluído
     * @param authentication Objeto de autenticação do Spring Security contendo os detalhes do usuário
     * @return ResponseEntity sem conteúdo (HTTP 204) indicando sucesso na exclusão
     */
    @Operation(summary = "Exclui um documento", description = "Exclui um documento específico com base no tipo e ID fornecidos.")
    @DeleteMapping("/{type}/{id}") // Ex: DELETE /documents/visit/45
    @PreAuthorize("hasAuthority('DELETE_REPORTS') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDocument(@PathVariable String type, @PathVariable Long id, Authentication authentication) {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User currentUser = userDetails.user();

        // Agora passamos o tipo e o ID para o serviço
        documentAggregationService.deleteDocumentByTypeAndId(type, id, currentUser);

        return ResponseEntity.noContent().build(); // Retorna 204 No Content (sucesso)
    }

    /**
     * Retorna os documentos mais recentes de todos os usuários do sistema (Acesso Administrativo).
     * Este endpoint é restrito a usuários com perfil ADMIN e retorna um histórico resumido
     * dos últimos documentos gerados no sistema, independente do técnico responsável.
     * A quantidade de documentos retornados é limitada aos mais recentes.
     *
     * @return ResponseEntity com uma lista limitada de DocumentSummaryDTO contendo os documentos mais recentes do sistema
     */
    @Operation(summary = "Retorna os documentos mais recentes", description = "Retorna os documentos mais recentes de todos os usuários do sistema (Acesso Administrativo).")
    @GetMapping("/latest/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DocumentSummaryDTO>> getLatestDocumentsForAdmin() {
        List<DocumentSummaryDTO> latestDocuments = documentAggregationService.findAllLatestDocumentsForAdmin();
        return ResponseEntity.ok(latestDocuments);
    }

    /**
     * Lista todos os documentos do sistema com filtros e paginação (Acesso Administrativo).
     * Este endpoint é restrito a usuários com perfil ADMIN e permite visualizar
     * todos os documentos registrados no sistema, independente do técnico responsável.
     * Suporta filtragem por tipo de documento, nome do cliente e intervalo de datas.
     *
     * @param type Filtro opcional por tipo de documento (visit, aep, risk)
     * @param clientName Filtro opcional por nome do cliente
     * @param startDate Filtro opcional de data inicial (formato AAAA-MM-DD)
     * @param endDate Filtro opcional de data final (formato AAAA-MM-DD)
     * @param pageable Objeto do Spring que contém informações de paginação (page= e size=)
     * @return ResponseEntity com uma Página (Page) de DocumentSummaryDTO contendo todos os documentos do sistema
     */
    @Operation(summary = "Lista todos os documentos", description = "Lista todos os documentos do sistema com filtros e paginação (Acesso Administrativo).")
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<DocumentSummaryDTO>> getAllDocumentsAdmin(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String clientName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable
    ) {
        Page<DocumentSummaryDTO> documentsPage = documentAggregationService.findAllDocumentsGlobal(
                type, clientName, startDate, endDate, pageable
        );
        return ResponseEntity.ok(documentsPage);
    }

    /**
     * Exporta todos os documentos do sistema em um arquivo ZIP.
     */
    @Operation(summary = "Exportar documentos (ZIP)", description = "Baixa todos os documentos do sistema em um arquivo .zip, podendo filtrar por intervalo de datas.")
    @GetMapping("/export/zip")
    @PreAuthorize("hasAuthority('VIEW_REPORT') or hasRole('ADMIN')")
    public void exportDocumentsAsZip(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletResponse response) throws IOException {

        // O serviço escreve direto no HttpServletResponse
        documentAggregationService.exportDocumentsToZip(startDate, endDate, response);
    }
}