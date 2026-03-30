-- Remove a trava de unicidade para permitir eventos fantasmas de reagendamento
ALTER TABLE tb_agenda_event DROP CONSTRAINT uk1cxkybxw9s9v1rwfe28ltq83;