-- Passo 1: Remove a constraint antiga
ALTER TABLE public.tb_user DROP CONSTRAINT IF EXISTS tb_user_role_check;

-- Passo 2: Atualiza os valores ANTES de criar a nova constraint
UPDATE public.tb_user SET role = 'ROLE_ADMIN' WHERE role = 'ADMIN';
UPDATE public.tb_user SET role = 'ROLE_USER' WHERE role = 'USER';

-- Passo 3: Agora cria a constraint nova (quando já não há dados antigos)
ALTER TABLE public.tb_user ADD CONSTRAINT tb_user_role_check CHECK ((role)::text = ANY ((ARRAY['ROLE_ADMIN'::character varying, 'ROLE_USER'::character varying])::text[]));