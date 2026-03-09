-- liquibase formatted sql

CREATE TABLE public.client_account ( bill numeric NULL, user_id uuid NOT NULL, owner_name varchar(255) NULL, CONSTRAINT client_account_pkey null);

ALTER TABLE public.client_account OWNER TO bank;
GRANT ALL ON TABLE public.client_account TO bank;


CREATE TABLE public.card ( balance numeric NULL, expiry_end date NOT NULL, account_id uuid NULL, id uuid NOT NULL, user_id uuid NOT NULL, status varchar(20) NULL, "number" varchar(255) NOT NULL, CONSTRAINT card_pkey PRIMARY KEY (id), CONSTRAINT card_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'BLOCKED'::character varying, 'EXPIRED'::character varying])::text[]))), CONSTRAINT fkylwkonivnxj328j23q25yc7g FOREIGN KEY (account_id) REFERENCES public.client_account(user_id));

ALTER TABLE public.card OWNER TO bank;
GRANT ALL ON TABLE public.card TO bank;