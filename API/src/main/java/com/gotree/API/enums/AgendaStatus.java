package com.gotree.API.enums;

import lombok.Getter;

@Getter
public enum AgendaStatus {
    A_CONFIRMAR("À Confirmar"),
    CONFIRMADO("Confirmado"),
    CANCELADO("Cancelado"),
    REAGENDADO("Reagendado"); // O status indica que houve mudança

    private final String descricao;

    AgendaStatus(String descricao) {
        this.descricao = descricao;
    }
}