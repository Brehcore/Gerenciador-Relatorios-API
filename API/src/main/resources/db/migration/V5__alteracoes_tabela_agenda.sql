-- 1. Remove a obrigatoriedade (NOT NULL) da coluna shift
ALTER TABLE tb_agenda_event ALTER COLUMN shift DROP NOT NULL;

-- 2. Adiciona as colunas para vínculo com Unidade e Setor
ALTER TABLE tb_agenda_event ADD COLUMN unit_id BIGINT;
ALTER TABLE tb_agenda_event ADD COLUMN sector_id BIGINT;

-- 3. Cria as chaves estrangeiras (Foreign Keys) para garantir a integridade dos dados
ALTER TABLE tb_agenda_event
    ADD CONSTRAINT fk_agenda_event_unit
        FOREIGN KEY (unit_id)
            REFERENCES tb_unit(id);

ALTER TABLE tb_agenda_event
    ADD CONSTRAINT fk_agenda_event_sector
        FOREIGN KEY (sector_id)
            REFERENCES tb_sector(id);

-- 4. Adiciona as colunas para controle de execução da visita (Realizada ou Não)
ALTER TABLE tb_agenda_event ADD COLUMN is_realized BOOLEAN;
ALTER TABLE tb_agenda_event ADD COLUMN non_completion_reason TEXT;