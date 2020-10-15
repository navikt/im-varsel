package no.nav.helse.inntektsmeldingsvarsel.brevutsendelse

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.helse.inntektsmeldingsvarsel.RecurringJob
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository.AltinnBrevUtsendelseRepository
import java.time.Duration
import java.time.LocalDateTime

class SendAltinnBrevUtsendelseJob(
        private val repo: AltinnBrevUtsendelseRepository,
        private val sender: AltinnBrevutsendelseSender,
        coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        waitTimeWhenEmptyQueue: Duration = Duration.ofMinutes(5)
) : RecurringJob(coroutineScope, waitTimeWhenEmptyQueue) {

    override fun doJob() {

        var isEmpty: Boolean
        do {
            val varslinger = repo.getNextBatch()
            isEmpty = varslinger.isEmpty()
            if (!isEmpty) {
                logger.info("Sender ${varslinger.size} brev")
            }

            varslinger.forEach {
                sender.send(it)
                repo.updateSentStatus(it.id, LocalDateTime.now(), true)
            }
        } while (!isEmpty)
    }

}