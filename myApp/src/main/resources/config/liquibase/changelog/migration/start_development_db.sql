-- liquibase formatted sql

-- changeset lybov:1

-- public.cards определение

-- Drop table

-- DROP TABLE public.cards;

CREATE TABLE public.cards (
	id uuid NOT NULL,
	balance numeric NULL,
	expiry_end date NOT NULL,
	"number" varchar(255) NOT NULL,
	user_id uuid NOT NULL,
	status varchar(20) NULL,
	CONSTRAINT cards_pkey PRIMARY KEY (id),
	CONSTRAINT cards_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'BLOCKED'::character varying, 'EXPIRED'::character varying])::text[])))
);

-- Permissions

ALTER TABLE public.cards OWNER TO bank;
GRANT ALL ON TABLE public.cards TO bank;