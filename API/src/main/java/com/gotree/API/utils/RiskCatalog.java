package com.gotree.API.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class RiskCatalog {

    @Data
    @AllArgsConstructor
    public static class RiskItem {
        private int code;
        private String type; // FÍSICO, QUÍMICO, etc.
        private String factor; // Descrição
    }

    public static final List<RiskItem> ALL_RISKS = Arrays.asList(
            new RiskItem(1, "FÍSICO", "Infrassom e sons de baixa frequência"),
            new RiskItem(2, "FÍSICO", "Ruído contínuo ou intermitente"),
            new RiskItem(3, "FÍSICO", "Ruído impulsivo ou de impacto"),
            // ... (Adicione aqui todos os 103 itens do seu PDF) ...
            // Exemplo do final da lista:
            new RiskItem(103, "ACIDENTE", "Procedimentos de ajuste, limpeza, manutenção e inspeção deficientes ou inexistentes")
    );

    public static RiskItem getByCode(int code) {
        return ALL_RISKS.stream().filter(r -> r.getCode() == code).findFirst().orElse(null);
    }
}