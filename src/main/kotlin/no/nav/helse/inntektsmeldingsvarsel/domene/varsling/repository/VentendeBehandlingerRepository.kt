package no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository

import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.SpleisInntektsmeldingMelding
import java.sql.Connection
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Inneholder en liste over saker som venter på IM.
 *
 * Hvis det kommer en melding om at saken ikke lenger krever IM skal den fjernes via remove()
 *
 */
interface VentendeBehandlingerRepository {
    /**
     * Benytter fnr, virksomhet og fom som nøkkel for å upserte en sak som venter på IM
     */
    fun insertIfNotExists(
            fnr: String,
            virksomhet: String,
            fom: LocalDate,
            tom: LocalDate,
            opprettet: LocalDateTime
    )

    /**
     * Fjerner alle ventende saker på den gitte nøkkelen
     */
    fun remove(fnr: String, virksomhet: String, con: Connection)

    /**
     * Finner alle saker som er endre enn den gitt datoen. Disse kan da grupperes på virksomhet for
     * å opprette varsler som sendes via altinn.
     */
    fun findOlderThan(date: LocalDateTime): Set<SpleisInntektsmeldingMelding>
}