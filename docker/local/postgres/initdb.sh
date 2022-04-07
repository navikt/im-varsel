#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER "im-varsel" WITH PASSWORD 'im-varsel';
    CREATE DATABASE "im-varsel";
    CREATE SCHEMA "im-varsel";
    GRANT ALL PRIVILEGES ON DATABASE "im-varsel" TO "im-varsel";
EOSQL

psql -v ON_ERROR_STOP=1 --username "im-varsel" --dbname "im-varsel" <<-EOSQL

    CREATE TABLE varsling (
        uuid varchar(64) NOT NULL primary key,
        sent bool NOT NULL,
        read bool NOT NULL DEFAULT false,
        opprettet timestamp NOT NULL,
        behandlet timestamp,
        lestTidspunkt timestamp,
        virksomhetsNr varchar(9) NOT NULL,
        virksomhetsNavn varchar(255) NOT NULL DEFAULT('Arbeidsgiver'),
        data jsonb NOT NULL
    );

   CREATE TABLE ventende_behandlinger (
        organisasjonsnummer varchar(9) NOT NULL,
        fødselsnummer varchar(11) NOT NULL,
        fom timestamp NOT NULL,
        tom timestamp NOT NULL,
        opprettet timestamp NOT NULL,
        constraint unik_periode_constraint UNIQUE(fødselsnummer, organisasjonsnummer, fom)
    );

    CREATE TABLE altinn_brev_mal (
        data jsonb NOT NULL
    );

    CREATE TABLE altinn_brev_utsendelse (
        id serial primary key,
        sent bool NOT NULL DEFAULT false,
        altinnBrevMalId varchar(64) NOT NULL,
        behandlet timestamp,
        virksomhetsNr varchar(9) NOT NULL
    );
EOSQL
