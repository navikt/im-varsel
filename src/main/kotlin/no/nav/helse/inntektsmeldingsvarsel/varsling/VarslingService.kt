package no.nav.helse.inntektsmeldingsvarsel.varsling

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.inntektsmeldingsvarsel.ANTALL_DUPLIKATMELDINGER
import no.nav.helse.inntektsmeldingsvarsel.AllowList
import no.nav.helse.inntektsmeldingsvarsel.domene.Periode
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.PersonVarsling
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.Varsling
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.MeldingsfilterRepository
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.VarslingRepository
import no.nav.helse.inntektsmeldingsvarsel.pdl.PdlClient
import no.nav.helse.inntektsmeldingsvarsel.pdl.fullName
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.ManglendeInntektsMeldingMelding
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class VarslingService(
        private val repository: VarslingRepository,
        private val mapper: VarslingMapper,
        private val om: ObjectMapper,
        private val hashRepo: MeldingsfilterRepository,
        private val pdlClient: PdlClient,
        private val allowList: AllowList
) {

    val logger = LoggerFactory.getLogger(VarslingService::class.java)

    fun finnNesteUbehandlede(max: Int, aggregatPeriode: String): List<Varsling> {
        return repository.findBySentStatus(false, max, aggregatPeriode).map { mapper.mapDomain(it) }
    }

    fun finnUleste(max: Int): List<Varsling> {
        return repository.findSentButUnread(max).map { mapper.mapDomain(it) }
    }

    fun oppdaterSendtStatus(varsling: Varsling, sendtStatus: Boolean) {
        logger.info("Oppdaterer sendt status på ${varsling.uuid} til $sendtStatus")
        repository.updateSentStatus(varsling.uuid, LocalDateTime.now(), sendtStatus)
    }

    fun lagre(varsling: Varsling) {
        repository.insert(mapper.mapDto(varsling))
    }

    fun oppdaterData(varsling: Varsling) {
        repository.updateData(varsling.uuid, mapper.mapDto(varsling).data)
    }

    fun slett(uuid: String) {
        repository.remove(uuid)
    }

    fun aggregate(jsonMessageString: String) {
        val kafkaMessage = om.readValue(jsonMessageString, ManglendeInntektsMeldingMelding::class.java)
        logger.info("Fikk en melding fra kafka på virksomhetsnummer ${kafkaMessage.organisasjonsnummer} fra ${kafkaMessage.opprettet}")
        val periodeHash = kafkaMessage.periodeHash()

        if (!allowList.isAllowed(kafkaMessage.organisasjonsnummer)) {
            logger.debug("Virksomheten er ikke tillatt")
            return
        }

        if (hashRepo.exists(periodeHash)) {
            logger.info("Denne periode er allerede sett")
            ANTALL_DUPLIKATMELDINGER.inc()
            return
        }

        val aggregateStrategy = resolveAggregationStrategy(kafkaMessage)
        val aggregatPeriode = aggregateStrategy.toPeriodeId(kafkaMessage.opprettet.toLocalDate())
        val existingAggregate =
                repository.findByVirksomhetsnummerAndPeriode(kafkaMessage.organisasjonsnummer, aggregatPeriode)

        val navn = pdlClient.person(kafkaMessage.fødselsnummer)?.fullName() ?: ""

        val person = PersonVarsling(
                navn,
                kafkaMessage.fødselsnummer,
                Periode(kafkaMessage.fom, kafkaMessage.tom),
                kafkaMessage.opprettet
        )

        if (existingAggregate == null) {
            logger.info("Det finnes ikke et aggregat på ${kafkaMessage.organisasjonsnummer} for periode $aggregatPeriode, lager en ny")
            val newEntry = Varsling(
                    aggregatPeriode,
                    kafkaMessage.organisasjonsnummer,
                    mutableSetOf(person)
            )
            repository.insert(mapper.mapDto(newEntry))
        } else {
            val  domainVarsling = mapper.mapDomain(existingAggregate)
            logger.info("Fant et aggregat på ${kafkaMessage.organisasjonsnummer} for $aggregatPeriode med ${domainVarsling.liste.size} personer")
            domainVarsling.liste.add(person)
            repository.updateData(domainVarsling.uuid, mapper.mapDto(domainVarsling).data)
        }

        hashRepo.insert(periodeHash)
    }

    /**
     * Finner strategien som skal brukes for å aggregere varsler for denne meldingen. Kan i fremtiden baseres på org-nr etc
     */
    private fun resolveAggregationStrategy(kafkaMessage: ManglendeInntektsMeldingMelding) = DailyVarslingStrategy()

    fun oppdaterLestStatus(varsling: Varsling, lestStatus: Boolean) {
        logger.info("Oppdaterer lest status på ${varsling.uuid} til $lestStatus")
        repository.updateReadStatus(varsling.uuid, lestStatus)
    }
}
