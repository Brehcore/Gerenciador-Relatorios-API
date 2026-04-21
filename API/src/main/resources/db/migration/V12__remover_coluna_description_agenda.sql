-- Remove a coluna antiga (OID/BIGINT)
ALTER TABLE tb_agenda_event DROP COLUMN description;

-- Cria a coluna nova com o tipo correto (TEXT)
ALTER TABLE tb_agenda_event ADD COLUMN description TEXT;