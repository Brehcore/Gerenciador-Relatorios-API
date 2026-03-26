package com.gotree.API.utils;

import org.springframework.web.util.HtmlUtils;

public class XmlSanitizer {

    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        // O HtmlUtils.htmlEscape é a ferramenta oficial do Spring para evitar XSS.
        // Ele converte <script> para &lt;script&gt;, neutralizando a ameaça e
        // avisando a IDE que o dado está limpo!
        return HtmlUtils.htmlEscape(input.trim());
    }
}