-- V2__atualizar_agenda_e_status.sql

-- 1. Adicionar as novas colunas necessárias para a nova lógica
ALTER TABLE public.tb_agenda_event ADD COLUMN status VARCHAR(255);
ALTER TABLE public.tb_agenda_event ADD COLUMN rescheduled_to_date DATE;

-- 2. Remover a constraint antiga que limitava os tipos de evento
-- O nome "tb_agenda_event_event_type_check" foi extraído do seu dump V1
ALTER TABLE public.tb_agenda_event DROP CONSTRAINT tb_agenda_event_event_type_check;

-- 3. Migração de Dados (Data Migration)
-- Passo A: Converter o antigo tipo 'VISITA_REAGENDADA' para o novo padrão
UPDATE public.tb_agenda_event 
SET event_type = 'VISITA_TECNICA', 
    status = 'REAGENDADO' 
WHERE event_type = 'VISITA_REAGENDADA';

-- Passo B: Definir status 'CONFIRMADO' para eventos manuais antigos (EVENTO, TREINAMENTO)
-- Isso evita que fiquem com status NULL
UPDATE public.tb_agenda_event 
SET status = 'CONFIRMADO' 
WHERE status IS NULL AND event_type IN ('EVENTO', 'TREINAMENTO');

-- Passo C: Caso exista algum outro registro perdido, define como 'A_CONFIRMAR' por segurança
UPDATE public.tb_agenda_event 
SET status = 'A_CONFIRMAR' 
WHERE status IS NULL;

-- 4. Recriar a Constraint de verificação com os NOVOS tipos permitidos
-- Agora aceita: EVENTO, TREINAMENTO e VISITA_TECNICA
ALTER TABLE public.tb_agenda_event 
ADD CONSTRAINT tb_agenda_event_event_type_check 
CHECK (event_type IN ('EVENTO', 'TREINAMENTO', 'VISITA_TECNICA'));

-- 5. (Opcional, mas recomendado) Criar Constraint para a coluna status também
ALTER TABLE public.tb_agenda_event 
ADD CONSTRAINT tb_agenda_event_status_check 
CHECK (status IN ('A_CONFIRMAR', 'CONFIRMADO', 'CANCELADO', 'REAGENDADO'));