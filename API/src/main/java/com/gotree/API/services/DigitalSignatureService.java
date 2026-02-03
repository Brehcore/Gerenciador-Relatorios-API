package com.gotree.API.services;

import com.gotree.API.entities.User;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class DigitalSignatureService {

    private final SymmetricCryptoService cryptoService;

    public DigitalSignatureService(SymmetricCryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public byte[] signPdf(byte[] originalPdfBytes, User signer) {
        if (signer.getCertificatePath() == null || signer.getCertificatePassword() == null) {
            throw new IllegalStateException("Usuário não possui certificado digital configurado.");
        }

        try {
            // 1. Carregar Certificado
            String password = cryptoService.decrypt(signer.getCertificatePassword());
            KeyStore ks = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(signer.getCertificatePath())) {
                ks.load(fis, password.toCharArray());
            }

            String alias = ks.aliases().nextElement();
            PrivateKey pk = (PrivateKey) ks.getKey(alias, password.toCharArray());
            Certificate[] chain = ks.getCertificateChain(alias);

            // --- NOVA LÓGICA: Extrair dados reais do Certificado (PF ou PJ) ---
            X509Certificate x509Cert = (X509Certificate) chain[0];
            String subjectName = extractCommonName(x509Cert); // Ex: "EMPRESA SA:12345678000100"
            // -----------------------------------------------------------------

            // 2. Preparar PDF
            PdfReader reader = new PdfReader(originalPdfBytes);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            PdfStamper stamper = PdfStamper.createSignature(reader, os, '\0');

            // 3. Aparência Criptográfica
            PdfSignatureAppearance appearance = stamper.getSignatureAppearance();
            appearance.setCrypto(pk, chain, null, PdfSignatureAppearance.WINCER_SIGNED);
            appearance.setReason("Validação Digital ICP-Brasil");
            appearance.setLocation("Brasil");

            // 4. Área Visual
            int totalPages = reader.getNumberOfPages();
            Rectangle signatureRect = new Rectangle(20, 20, 575, 110);
            appearance.setVisibleSignature(signatureRect, totalPages, "sig_icp_" + System.currentTimeMillis());

            // 5. Desenho Manual
            PdfTemplate layer = appearance.getLayer(2);
            float rectHeight = signatureRect.getHeight();

            // A. Ícone
            float imageWidth = 0;
            try {
                ClassPathResource imgResource = new ClassPathResource("images/stamp_icp.png");
                if (imgResource.exists()) {
                    try (InputStream is = imgResource.getInputStream()) {
                        byte[] imgBytes = is.readAllBytes();
                        Image img = Image.getInstance(imgBytes);
                        float imgSize = rectHeight - 10;
                        img.scaleToFit(imgSize, imgSize);
                        img.setAbsolutePosition(5, (rectHeight - img.getScaledHeight()) / 2);
                        layer.addImage(img);
                        imageWidth = img.getScaledWidth() + 25;
                    }
                }
            } catch (Exception e) {
                imageWidth = 5;
            }

            // B. Texto Dinâmico (Adapta para PF ou PJ)
            String data = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            layer.beginText();
            layer.setFontAndSize(bf, 10);
            layer.setRGBColorFill(0, 0, 0);

            float middleY = rectHeight / 2;
            float lineHeight = 15;

            // Linha 1: "Assinado por: NOME:DOCUMENTO" (Vem direto do certificado)
            layer.setTextMatrix(imageWidth, middleY + lineHeight);
            // O Common Name (CN) no Brasil geralmente já tem o formato "NOME:CPF/CNPJ"
            // Vamos formatar para ficar bonito, trocando os ":" por " - " se houver
            String nomeFormatado = subjectName.replace(":", " - Doc: ");
            layer.showText("Assinado digitalmente por: " + nomeFormatado);

            // Linha 2: Data
            layer.setTextMatrix(imageWidth, middleY);
            layer.showText("Data da Assinatura: " + data);

            // Linha 3: Aviso
            layer.setTextMatrix(imageWidth, middleY - lineHeight);
            layer.setRGBColorFill(100, 100, 100);
            layer.showText("Verifique a autenticidade em: https://validar.iti.gov.br.");

            layer.endText();

            stamper.close();
            return os.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao assinar digitalmente: " + e.getMessage(), e);
        }
    }

    /**
     * Extrai o Common Name (CN) do certificado.
     * No padrão ICP-Brasil, o CN contém "Nome:CPF" ou "Razão Social:CNPJ".
     */
    private String extractCommonName(X509Certificate cert) {
        try {
            String dn = cert.getSubjectX500Principal().getName();
            LdapName ldapDN = new LdapName(dn);
            for (Rdn rdn : ldapDN.getRdns()) {
                if (rdn.getType().equalsIgnoreCase("CN")) {
                    return rdn.getValue().toString();
                }
            }
            return "Desconhecido";
        } catch (Exception e) {
            return "Assinatura Digital";
        }
    }
}