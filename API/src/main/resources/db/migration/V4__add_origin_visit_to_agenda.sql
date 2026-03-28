-- 1. Cria a coluna e a chave estrangeira
ALTER TABLE tb_agenda_event ADD COLUMN origin_technical_visit_id BIGINT NULL;
ALTER TABLE tb_agenda_event ADD CONSTRAINT fk_agenda_visit
    FOREIGN KEY (origin_technical_visit_id) REFERENCES tb_technical_visit(id);

-- 2. Popula a agenda com o histórico (Migração)
INSERT INTO tb_agenda_event (title, event_date, shift, user_id, company_id, event_type, status, origin_technical_visit_id)
SELECT
    CONCAT('Próxima Visita: ', c.name),
    tv.next_visit_date,
    COALESCE(tv.next_visit_shift, 'MANHA'), -- A MÁGICA ACONTECE AQUI!
    tv.technician_id,
    tv.client_company_id,
    'VISITA_TECNICA',
    'A_CONFIRMAR',
    tv.id
FROM tb_technical_visit tv
         JOIN tb_company c ON tv.client_company_id = c.id
WHERE tv.next_visit_date IS NOT NULL
  AND tv.id NOT IN (SELECT origin_technical_visit_id FROM tb_agenda_event WHERE origin_technical_visit_id IS NOT NULL);