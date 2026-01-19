package com.gotree.API.utils;

public class XmlSanitizer {

    /**
     * Remove caracteres de controle XML inválidos de uma string.
     * Isso previne erros no Flying Saucer ao gerar PDFs.
     *
     * @param input A string a ser limpa
     * @return A string sem caracteres inválidos ou null se a entrada for null
     */
    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        // Regex para remover caracteres de controle XML inválidos (exceto tab, line feed, carriage return)
        // XML 1.0 permite: #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
        // Remove caracteres de controle como 0x2 (STX), 0x3 (ETX), etc.
        return input.replaceAll("[^\\u0009\\u000A\\u000D\\u0020-\\uD7FF\\uE000-\\uFFFD\\u10000-\\u10FFFF]+", "");
    }
}