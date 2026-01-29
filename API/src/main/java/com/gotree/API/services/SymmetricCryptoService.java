package com.gotree.API.services;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Serviço para criptografia simétrica (AES).
 * Usado para criptografar a senha do certificado digital antes de salvar no banco,
 * permitindo que ela seja recuperada (descriptografada) no momento da assinatura.
 */
@Service
public class SymmetricCryptoService {

    // Em produção, esta chave deve vir do application.properties (@Value)
    // Deve ter exatos 16, 24 ou 32 caracteres para AES
    private static final String SECRET_KEY = "GoTreeSecretKey!";
    private static final String ALGORITHM = "AES";

    public String encrypt(String data) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao criptografar dados sensíveis.", e);
        }
    }

    public String decrypt(String encryptedData) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
            return new String(cipher.doFinal(decodedBytes));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao descriptografar dados sensíveis.", e);
        }
    }
}