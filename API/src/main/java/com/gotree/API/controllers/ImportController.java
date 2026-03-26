package com.gotree.API.controllers;

import com.gotree.API.services.ExcelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Controlador responsável por gerenciar operações de importação e exportação de dados de empresas.
 * Fornece endpoints para importar empresas de arquivos Excel e exportar empresas para formato Excel.
 * O acesso é restrito a usuários com papel ADMIN.
 */
@Tag(name = "Importação e Exportação de Empresas", description = "Gerenciamento de importação e exportação de empresas")
@RestController
@RequestMapping("/import")
public class ImportController {

    private final ExcelService excelService;

    public ImportController(ExcelService excelService) {
        this.excelService = excelService;
    }

    /**
     * Importa empresas de um arquivo Excel enviado.
     * Apenas usuários com papel ADMIN podem acessar este endpoint.
     *
     * @param file o arquivo Excel contendo dados de empresas a serem importados
     * @return ResponseEntity com mensagem de sucesso se a importação for concluída com êxito,
     * ou mensagem de erro se uma exceção ocorrer durante o processo
     */
    @Operation(summary = "Importa empresas de um arquivo Excel", description = "Importa empresas de um arquivo Excel para o sistema.")
    @PostMapping("/companies")
    @PreAuthorize("hasRole('ADMIN')") // Apenas admin deve importar
    public ResponseEntity<String> importCompanies(@RequestParam("file") MultipartFile file) {
        try {
            excelService.importCompanies(file);
            return ResponseEntity.ok("Importação concluída com sucesso!");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Erro ao ler o arquivo: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro na importação: " + e.getMessage());
        }
    }

    /**
     * Exporta todas as empresas para um arquivo Excel.
     * Apenas usuários com papel ADMIN podem acessar este endpoint.
     *
     * @return ResponseEntity contendo o arquivo Excel como um recurso para download
     * @throws IOException se ocorrer um erro durante o processo de geração do arquivo
     */
    @Operation(summary = "Exporta todas as empresas para um arquivo Excel", description = "Exporta todas as empresas do sistema para um arquivo Excel.")
    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InputStreamResource> exportCompanies() throws IOException {
        ByteArrayInputStream in = excelService.exportCompaniesToExcel(); // Ou excelService

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=empresas_exportadas.xlsx");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }
}