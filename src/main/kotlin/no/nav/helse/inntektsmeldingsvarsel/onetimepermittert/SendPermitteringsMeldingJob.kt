package no.nav.helse.inntektsmeldingsvarsel.onetimepermittert

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.helse.inntektsmeldingsvarsel.RecurringJob
import no.nav.helse.inntektsmeldingsvarsel.onetimepermittert.permisjonsvarsel.repository.PermisjonsvarselRepository
import java.time.Duration
import java.time.LocalDateTime

class SendPermitteringsMeldingJob(
        private val repo: PermisjonsvarselRepository,
        private val sender: AltinnPermisjonsVarselSender,
        coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        waitTimeWhenEmptyQueue: Duration = Duration.ofMinutes(5)
) : RecurringJob(coroutineScope, waitTimeWhenEmptyQueue) {

    override fun doJob() {

        var isEmpty: Boolean
        do {
            val varslinger = repo.getNextBatch()
            isEmpty = varslinger.isEmpty()
            logger.info("Sender ${varslinger.size} varsler")

            varslinger.forEach {
                sender.send(it)
                repo.updateSentStatus(it.id, LocalDateTime.now(), true)
            }
        } while (!isEmpty)
    }

}