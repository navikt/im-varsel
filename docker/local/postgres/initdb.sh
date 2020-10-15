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
        aggregatPeriode varchar(64) NOT NULL,
        virksomhetsNr varchar(9) NOT NULL,
        data jsonb NOT NULL
    );

    CREATE TABLE meldingsfilter (
        hash varchar(64) NOT NULL UNIQUE
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
