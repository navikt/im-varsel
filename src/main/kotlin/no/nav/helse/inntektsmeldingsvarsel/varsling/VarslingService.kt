package no.nav.helse.inntektsmeldingsvarsel.varsling

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.inntektsmeldingsvarsel.ANTALL_FILTRERTE_VARSLER
import no.nav.helse.inntektsmeldingsvarsel.AllowList
import no.nav.helse.inntektsmeldingsvarsel.domene.Periode
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.PersonVarsling
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.Varsling
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.VentendeBehandlingerRepository
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.VarslingRepository
import no.nav.helse.inntektsmeldingsvarsel.integrasjon.brreg.BrregClient
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.SpleisInntektsmeldingMelding
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.SpleisInntektsmeldingMelding.Meldingstype.TRENGER_IKKE_INNTEKTSMELDING
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.SpleisInntektsmeldingMelding.Meldingstype.TRENGER_INNTEKTSMELDING
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import javax.sql.DataSource

class VarslingService(
    private val datasource: DataSource,
    private val varselRepository: VarslingRepository,
    private val ventendeRepo: VentendeBehandlingerRepository,
    private val mapper: VarslingMapper,
    private val om: ObjectMapper,
    private val pdlClient: PdlClient,
    private val allowList: AllowList,
    private val brregClient: BrregClient
) {

    val logger = LoggerFactory.getLogger(VarslingService::class.java)

    fun finnNesteUbehandlede(max: Int): List<Varsling> {
        return varselRepository.findBySentStatus(false, max).map { mapper.mapDomain(it) }
    }

    fun finnSisteUleste(max: Int): List<Varsling> {
        return varselRepository.findSentButUnread(max).map { mapper.mapDomain(it) }
    }

    fun oppdaterSendtStatus(varsling: Varsling, sendtStatus: Boolean) {
        logger.debug("Oppdaterer sendt status på ${varsling.uuid} til $sendtStatus")
        varselRepository.updateSentStatus(varsling.uuid, LocalDateTime.now(), sendtStatus)
    }

    fun lagre(varsling: Varsling) {
        datasource.connection.use {
            varselRepository.insert(mapper.mapDto(varsling), it)
        }
    }

    fun oppdaterData(varsling: Varsling) {
        varselRepository.updateData(varsling.uuid, mapper.mapDto(varsling).data)
    }

    fun slett(uuid: String) {
        varselRepository.remove(uuid)
    }

    fun handleMessage(jsonMessageString: String) {
        val msg = om.readValue(jsonMessageString, SpleisInntektsmeldingMelding::class.java)
        logger.debug("Fikk en melding fra kafka på virksomhetsnummer ${msg.organisasjonsnummer} fra ${msg.opprettet}")

        when (msg.type) {
            TRENGER_INNTEKTSMELDING -> ventendeRepo.insertIfNotExists(msg.fødselsnummer, msg.organisasjonsnummer, msg.fom, msg.tom, msg.opprettet)
            TRENGER_IKKE_INNTEKTSMELDING -> datasource.connection.use {
                ventendeRepo.remove(msg.fødselsnummer, msg.organisasjonsnummer, it)
            }
        }
    }

    fun opprettVarslingerFraVentendeMeldinger() {
        ventendeRepo.findOlderThan(LocalDateTime.now().minusDays(Companion.VENTETID_I_DAGER))
            .groupBy { it.organisasjonsnummer }
            .map { gruppe ->
                Varsling(
                    virksomhetsNr = gruppe.key,
                    virksomhetsNavn = hentVirksomhetsNavn(gruppe.key),
                    liste = gruppe.value.map {
                        val navn = hentNavn(it)
                        PersonVarsling(
                            navn,
                            it.fødselsnummer,
                            Periode(it.fom, it.tom),
                            it.opprettet
                        )
                    }.toMutableSet()
                )
            }.forEach { varsling ->

                datasource.connection.use { con ->
                    if (allowList.isAllowed(varsling.virksomhetsNr)) {
                        varselRepository.insert(mapper.mapDto(varsling), con)
                    } else {
                        ANTALL_FILTRERTE_VARSLER.inc()
                        logger.debug("Virksomheten er ikke tillatt")
                    }

                    varsling.liste
                        .forEach { ventendeRepo.remove(it.personnumer, varsling.virksomhetsNr, con) }
                }
            }
    }

    private fun hentNavn(it: SpleisInntektsmeldingMelding): String {
        val pdlResponse = pdlClient.personNavn(it.fødselsnummer)?.navn?.firstOrNull()
        val navn = if (pdlResponse != null) "${pdlResponse.fornavn} ${pdlResponse.etternavn}" else ""
        return navn
    }

    private fun hentVirksomhetsNavn(orgnr: String): String {
        return runBlocking { brregClient.getVirksomhetsNavn(orgnr) }
    }

    fun oppdaterLestStatus(varsling: Varsling, lestStatus: Boolean) {
        logger.debug("Oppdaterer lest status på ${varsling.uuid} til $lestStatus")
        varselRepository.updateReadStatus(varsling.uuid, lestStatus)
    }

    companion object {
        val VENTETID_I_DAGER = 21L
    }
}
