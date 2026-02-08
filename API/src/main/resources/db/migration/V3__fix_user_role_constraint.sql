-- Remove a constraint antiga que valida ADMIN/USER
ALTER TABLE public.tb_user DROP CONSTRAINT tb_user_role_check;

-- Adiciona nova constraint que aceita ROLE_ADMIN/ROLE_USER
ALTER TABLE public.tb_user
    ADD CONSTRAINT tb_user_role_check CHECK ((role)::text = ANY ((ARRAY['ROLE_ADMIN'::character varying, 'ROLE_USER'::character varying])::text[]));

-- Atualiza registros existentes
UPDATE public.tb_user SET role = 'ROLE_ADMIN' WHERE role = 'ADMIN';
UPDATE public.tb_user SET role = 'ROLE_USER' WHERE role = 'USER';