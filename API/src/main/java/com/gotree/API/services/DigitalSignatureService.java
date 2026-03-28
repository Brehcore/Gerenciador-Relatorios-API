package com.gotree.API.services;

import com.gotree.API.entities.User;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfSignatureAppearance;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfTemplate;
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
import java.util.Arrays;

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

        char[] passwordChars = null;

        try {
            // 1. Extração da senha e isolamento em char[] (LGPD/Segurança)
            String decryptedPassword = cryptoService.decrypt(signer.getCertificatePassword());
            passwordChars = decryptedPassword.toCharArray();

            KeyStore ks = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(signer.getCertificatePath())) {
                ks.load(fis, passwordChars);
            }

            String alias = ks.aliases().nextElement();
            PrivateKey pk = (PrivateKey) ks.getKey(alias, passwordChars);
            Certificate[] chain = ks.getCertificateChain(alias);

            // --- 🚨 DIAGNÓSTICO CRÍTICO DA CADEIA ---
            System.out.println("Tamanho da cadeia lida do .pfx: " + chain.length);
            if (chain.length == 1) {
                System.err.println("ALERTA: O seu arquivo .pfx não contém a cadeia completa (Raiz e Intermediária). A assinatura terá uma interrogação no Adobe!");
            }

            X509Certificate x509Cert = (X509Certificate) chain[0];
            String subjectName = extractCommonName(x509Cert);

            PdfReader reader = new PdfReader(originalPdfBytes);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            PdfStamper stamper = PdfStamper.createSignature(reader, os, '\0');

            PdfSignatureAppearance appearance = stamper.getSignatureAppearance();

            // 2. WINCER_SIGNED é o padrão para PKCS#7 Detached no OpenPDF
            appearance.setCrypto(pk, chain, null, PdfSignatureAppearance.WINCER_SIGNED);
            appearance.setReason("Validação Digital ICP-Brasil");
            appearance.setLocation("Brasil");

            // 3. Renderização Visual da Caixa de Assinatura
            int totalPages = reader.getNumberOfPages();
            Rectangle signatureRect = new Rectangle(20, 20, 575, 110);
            appearance.setVisibleSignature(signatureRect, totalPages, "sig_icp_" + System.currentTimeMillis());

            PdfTemplate layer = appearance.getLayer(2);
            float rectHeight = signatureRect.getHeight();
            float imageWidth = 5;

            try {
                ClassPathResource imgResource = new ClassPathResource("images/stamp_icp.png");
                if (imgResource.exists()) {
                    try (InputStream is = imgResource.getInputStream()) {
                        Image img = Image.getInstance(is.readAllBytes());
                        float imgSize = rectHeight - 10;
                        img.scaleToFit(imgSize, imgSize);
                        img.setAbsolutePosition(5, (rectHeight - img.getScaledHeight()) / 2);
                        layer.addImage(img);
                        imageWidth = img.getScaledWidth() + 25;
                    }
                }
            } catch (Exception ignored) {}

            String data = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);

            layer.beginText();
            layer.setFontAndSize(bf, 10);
            layer.setRGBColorFill(0, 0, 0);

            float middleY = rectHeight / 2;
            float lineHeight = 15;

            String nomeFormatado = subjectName.replace(":", " - Doc: ");
            layer.setTextMatrix(imageWidth, middleY + lineHeight);
            layer.showText("Assinado digitalmente por: " + nomeFormatado);

            layer.setTextMatrix(imageWidth, middleY);
            layer.showText("Data da Assinatura: " + data);

            layer.setTextMatrix(imageWidth, middleY - lineHeight);
            layer.setRGBColorFill(100, 100, 100);
            layer.showText("Verifique a autenticidade em: https://validar.iti.gov.br.");
            layer.endText();

            stamper.close();
            return os.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar assinatura: " + e.getMessage(), e);
        } finally {
            // Limpa a senha da memória (proteção contra memory dumps)
            if (passwordChars != null) {
                Arrays.fill(passwordChars, '0');
            }
        }
    }

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
            return "Assinatura Digital ICP-Brasil";
        }
    }
}