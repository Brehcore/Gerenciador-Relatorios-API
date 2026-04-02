-- Remove as colunas de agendamento futuro da tabela de relatórios de visita
-- A partir de agora, o futuro é controlado exclusivamente pela tb_agenda_event
ALTER TABLE tb_technical_visit DROP COLUMN next_visit_date;
ALTER TABLE tb_technical_visit DROP COLUMN next_visit_shift;