-- Remove a trava antiga do Enum que impedia a inserção de novos tipos como PERICIA
ALTER TABLE tb_agenda_event DROP CONSTRAINT IF EXISTS tb_agenda_event_event_type_check;