-- ==============================================================================
-- 1. REVERSÃO DA V8: Remoção da coluna is_draft
-- ==============================================================================
-- Usamos IF EXISTS por segurança, caso o banco esteja sendo recriado do zero
ALTER TABLE tb_technical_visit DROP COLUMN IF EXISTS is_draft;


-- ==============================================================================
-- 2. CRIAÇÃO DAS TABELAS DE PERFIL DE ACESSO (RBAC)
-- ==============================================================================

-- Cria a tabela principal de Perfis (AccessProfile)
CREATE TABLE access_profiles (
                                 id BIGSERIAL PRIMARY KEY,
                                 name VARCHAR(255) NOT NULL UNIQUE
);

-- Cria a tabela associativa para a lista de permissões (@ElementCollection)
CREATE TABLE profile_permissions (
                                     profile_id BIGINT NOT NULL,
                                     permission VARCHAR(100) NOT NULL,
                                     CONSTRAINT fk_profile_permissions_profile_id
                                         FOREIGN KEY (profile_id)
                                             REFERENCES access_profiles (id)
                                             ON DELETE CASCADE
);

-- ==============================================================================
-- 3. ATUALIZAÇÃO DA TABELA DE USUÁRIOS
-- ==============================================================================

-- Trocando 'users' para 'tb_users' (verifique se é este mesmo o nome da sua tabela!)
ALTER TABLE tb_user ADD COLUMN access_profile_id BIGINT;

ALTER TABLE tb_user ADD CONSTRAINT fk_tb_users_access_profile_id
    FOREIGN KEY (access_profile_id)
        REFERENCES access_profiles (id)
        ON DELETE SET NULL;