package com.gotree.API.services;

import com.lowagie.text.pdf.BaseFont;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

/**
 * Serviço responsável pela geração de relatórios em formato PDF a partir de templates HTML.
 * Utiliza Thymeleaf para processamento de templates e Flying Saucer para conversão HTML para PDF.
 */
@Service
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    private final TemplateEngine templateEngine;

    public ReportService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * Gera um documento PDF a partir de um template HTML processado com Thymeleaf.
     *
     * @param templateName Nome do arquivo de template HTML a ser processado
     * @param data         Mapa contendo as variáveis a serem utilizadas no template
     * @return Array de bytes contendo o documento PDF gerado
     * @throws RuntimeException se ocorrer algum erro durante a geração do PDF ou carregamento de fontes
     */
    public byte[] generatePdfFromHtml(String templateName, Map<String, Object> data) {
        Context context = new Context();
        context.setVariables(data);

        logger.info("Gerando HTML para o template: {}", templateName);
        String htmlContent = templateEngine.process(templateName, context);
        logger.debug("Conteúdo HTML gerado:\n{}", htmlContent); // Loga o HTML completo para depuração

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();

            // Bloco da fonte (sabemos que funciona, mas mantemos para consistência)
            try {
                URL fontUrl = getClass().getResource("/fonts/Montserrat.ttf");
                if (fontUrl == null) {
                    throw new IOException("Arquivo da fonte '/fonts/Montserrat.ttf' não encontrado.");
                }
                renderer.getFontResolver().addFont(fontUrl.toString(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception e) {
                logger.error("Falha ao carregar a fonte:", e);
                throw new RuntimeException("Falha ao carregar a fonte Montserrat.", e);
            }

            // Configuração do Base URI para encontrar imagens locais
            String baseUri = new File("/").toURI().toString();
            renderer.setDocumentFromString(htmlContent, baseUri);

            logger.info("Renderizador configurado com Base URI: {}. Executando layout...", baseUri);
            renderer.layout(); // O erro provavelmente acontece aqui

            logger.info("Layout concluído. Criando PDF...");
            renderer.createPDF(outputStream); // Ou aqui

            logger.info("PDF gerado com sucesso.");
            return outputStream.toByteArray();
        } catch (Exception e) {
            // ESTE É O LOG QUE PRECISAMOS. Ele mostrará a exceção original.
            logger.error("==== FALHA CRÍTICA NA GERAÇÃO DO PDF ====", e);
            throw new RuntimeException("Erro ao renderizar o PDF. Verifique o log de erro para a causa raiz.", e);
        }
    }
}