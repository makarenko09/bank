-- liquibase formatted sql

-- changeset lybov:1

CREATE TABLE public.card (
	balance numeric NULL,
	client_id uuid NULL,
	id uuid NOT NULL,
	user_id uuid NOT NULL,
	status varchar(20) NULL,
	"number" varchar(255) NOT NULL,
	expiry_end date NOT NULL,
	CONSTRAINT card_pkey PRIMARY KEY (id),
	CONSTRAINT card_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'BLOCKED'::character varying, 'EXPIRED'::character varying])::text[])))
);

-- changeset lybov:1

CREATE TABLE public.card (
	balance numeric NULL,
	client_id uuid NULL,
	id uuid NOT NULL,
	user_id uuid NOT NULL,
	status varchar(20) NULL,
	"number" varchar(255) NOT NULL,
	expiry_end date NOT NULL,
	CONSTRAINT card_pkey PRIMARY KEY (id),
	CONSTRAINT card_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'BLOCKED'::character varying, 'EXPIRED'::character varying])::text[])))
);

ALTER TABLE public.card ADD CONSTRAINT fkf8f6kgcoayr4q4ob5v0jdh48l FOREIGN KEY (client_id) REFERENCES public.client_account(user_id);
