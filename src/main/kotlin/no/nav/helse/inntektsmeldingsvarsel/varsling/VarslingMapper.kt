package no.nav.helse.inntektsmeldingsvarsel.varsling

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.Varsling
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.VarslingDbEntity

class VarslingMapper (private val mapper: ObjectMapper) {

    fun mapDto(varsling: Varsling): VarslingDbEntity {
        return VarslingDbEntity(
                uuid = varsling.uuid,
                data = mapper.writeValueAsString(varsling.liste),
                sent = varsling.varslingSendt,
                opprettet = varsling.opprettet,
                virksomhetsNr = varsling.virksomhetsNr,
                read = varsling.varslingLest
        )
    }

    fun mapDomain(dbEntity: VarslingDbEntity): Varsling {
        return Varsling(
                virksomhetsNr = dbEntity.virksomhetsNr,
                uuid = dbEntity.uuid,
                opprettet = dbEntity.opprettet,
                varslingSendt = dbEntity.sent,
                varslingLest = dbEntity.read,
                liste = mapper.readValue(dbEntity.data)
        )
    }
}