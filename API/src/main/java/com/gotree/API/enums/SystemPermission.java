package com.gotree.API.enums;

import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonFormat;

@Getter
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum SystemPermission {

    // --- MÓDULO AGENDA ---
    VIEW_AGENDA("Agenda", "Visualizar eventos na agenda"),
    CREATE_AGENDA("Agenda", "Criar novos eventos"),
    EDIT_AGENDA("Agenda", "Editar eventos existentes"),
    DELETE_AGENDA("Agenda", "Excluir eventos"),

    // --- MÓDULO CLIENTES ---
    VIEW_CLIENTS("Clientes", "Visualizar lista e detalhes de clientes"),
    CREATE_CLIENTS("Clientes", "Cadastrar novos clientes"),
    EDIT_CLIENTS("Clientes", "Editar dados de clientes"),
    DELETE_CLIENTS("Clientes", "Excluir clientes do sistema"),

    // --- MÓDULO RELATÓRIOS (SST) ---
    VIEW_REPORTS("Relatórios", "Visualizar relatórios emitidos"),
    CREATE_REPORTS("Relatórios", "Emitir/Criar novos relatórios"),
    EDIT_REPORTS("Relatórios", "Editar relatórios (se permitido por regra de negócio)"),
    DELETE_REPORTS("Relatórios", "Excluir ou cancelar relatórios"),

    // --- MÓDULO USUÁRIOS E PERFIS (ADMINISTRAÇÃO) ---
    VIEW_USERS("Administração", "Visualizar usuários e perfis"),
    CREATE_USERS("Administração", "Cadastrar novos usuários e criar perfis"),
    EDIT_USERS("Administração", "Editar usuários e alterar permissões de perfis"),
    DELETE_USERS("Administração", "Excluir usuários e perfis"),

    // --- MÓDULO EMPRESAS (UNIDADES E SETORES) ---
    VIEW_COMPANIES("Empresas", "Visualizar empresas, unidades, setores e cargos"),
    CREATE_COMPANIES("Empresas", "Cadastrar novos empresas, unidades, setores e cargos"),
    EDIT_COMPANIES("Empresas", "Editar empresas, unidades, setores e cargos"),
    DELETE_COMPANIES("Empresas", "Excluir empresas, unidades, setores e cargos"),

    // --- MÓDULO DASHBOARDS ---
    VIEW_DASHBOARDS("Dashboards", "Visualizar dashboards");

    private final String category;
    private final String description;
    private final String name;

    SystemPermission(String category, String description) {
        this.category = category;
        this.description = description;
        this.name = this.name();
    }
}