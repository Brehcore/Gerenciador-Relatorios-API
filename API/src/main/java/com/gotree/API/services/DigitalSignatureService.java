package com.gotree.API.services;

import com.gotree.API.entities.User;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class DigitalSignatureService {

    private final SymmetricCryptoService cryptoService;

    public DigitalSignatureService(SymmetricCryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    /**
     * Assina um PDF com o certificado digital do usuário, desenhando o selo e o texto manualmente.
     * Compatível com iText 2.1.7.
     *
     * @param originalPdfBytes O PDF original em bytes
     * @param signer           O usuário que está assinando
     * @return Bytes do PDF assinado
     */
    public byte[] signPdf(byte[] originalPdfBytes, User signer) {
        if (signer.getCertificatePath() == null || signer.getCertificatePassword() == null) {
            throw new IllegalStateException("Usuário não possui certificado digital configurado.");
        }

        try {
            // 1. Descriptografar a senha e carregar o KeyStore
            String password = cryptoService.decrypt(signer.getCertificatePassword());
            KeyStore ks = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(signer.getCertificatePath())) {
                ks.load(fis, password.toCharArray());
            }

            String alias = ks.aliases().nextElement();
            PrivateKey pk = (PrivateKey) ks.getKey(alias, password.toCharArray());
            Certificate[] chain = ks.getCertificateChain(alias);

            // 2. Preparar PDF
            PdfReader reader = new PdfReader(originalPdfBytes);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            // '\0' mantém a versão original do PDF
            PdfStamper stamper = PdfStamper.createSignature(reader, os, '\0');

            // 3. Configurar Aparência Criptográfica
            PdfSignatureAppearance appearance = stamper.getSignatureAppearance();
            appearance.setCrypto(pk, chain, null, PdfSignatureAppearance.WINCER_SIGNED);
            appearance.setReason("Validação Digital ICP-Brasil");
            appearance.setLocation("Brasil");

            // 4. Configurar Área Visual (Retângulo Largo no Rodapé)
            // Aumentando altura para 90 para dar mais respiro ao texto
            int totalPages = reader.getNumberOfPages();
            Rectangle signatureRect = new Rectangle(20, 20, 575, 110);
            appearance.setVisibleSignature(signatureRect, totalPages, "sig_icp_" + System.currentTimeMillis());

            // 5. DESENHO MANUAL NA CAMADA 2 (Visual)
            PdfTemplate layer = appearance.getLayer(2);
            float rectHeight = signatureRect.getHeight();

            // --- A. Desenhar o Ícone (Selo) ---
            float imageWidth = 0;
            try {
                // IMPORTANTE: Certifique-se que o nome do arquivo na pasta resources/images é exatamente este
                ClassPathResource imgResource = new ClassPathResource("images/stamp_icp.png");

                if (imgResource.exists()) {
                    // Lê os bytes diretamente para evitar problemas de URL em JARs
                    try (InputStream is = imgResource.getInputStream()) {
                        byte[] imgBytes = is.readAllBytes();
                        Image img = Image.getInstance(imgBytes);

                        // Escala a imagem para caber na altura (deixa margem de 10px)
                        float imgSize = rectHeight - 10;
                        img.scaleToFit(imgSize, imgSize);

                        // Posiciona no canto esquerdo absoluto do retângulo (X=5)
                        // Centraliza verticalmente
                        img.setAbsolutePosition(5, (rectHeight - img.getScaledHeight()) / 2);

                        layer.addImage(img);
                        // Define onde o texto começa: largura da imagem + 25px de margem
                        imageWidth = img.getScaledWidth() + 25;
                    }
                }
            } catch (Exception e) {
                System.err.println("Erro ao carregar logo da assinatura: " + e.getMessage());
                // Se falhar, o texto começa do canto esquerdo com pequena margem
                imageWidth = 5;
            }

            // --- B. Desenhar o Texto ---
            String nome = signer.getName().toUpperCase();
            String cpf = mascararCpf(signer.getCpf());
            String data = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            layer.beginText();
            layer.setFontAndSize(bf, 10); // Fonte 10 para melhor leitura
            layer.setRGBColorFill(0, 0, 0);

            // Ajuste vertical e espaçamento entre linhas
            float middleY = rectHeight / 2;
            float lineHeight = 15; // Espaçamento maior para não ficar apertado

            // Linha 1: Nome (Acima do meio)
            layer.setTextMatrix(imageWidth, middleY + lineHeight);
            layer.showText("Assinado digitalmente por: " + nome);

            // Linha 2: CPF e Data (No meio)
            layer.setTextMatrix(imageWidth, middleY);
            layer.showText("CPF: " + cpf + "  |  Data: " + data);

            // Linha 3: Aviso legal (Abaixo do meio)
            layer.setTextMatrix(imageWidth, middleY - lineHeight);
            layer.setRGBColorFill(100, 100, 100); // Cinza
            layer.showText("Verifique a autenticidade deste documento via ICP-Brasil.");

            layer.endText();

            // 6. Finalizar
            stamper.close();

            return os.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao assinar digitalmente: " + e.getMessage(), e);
        }
    }

    private String mascararCpf(String cpf) {
        if (cpf == null || cpf.length() < 11) return "***.***.***-**";
        String limpo = cpf.replaceAll("\\D", "");
        if (limpo.length() != 11) return cpf;
        return "***." + limpo.substring(3, 6) + "." + limpo.substring(6, 9) + "-**";
    }
}