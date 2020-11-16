package no.nav.helse.inntektsmeldingsvarsel.varsling.mottak

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.helse.inntektsmeldingsvarsel.AllowList
import no.nav.helse.inntektsmeldingsvarsel.varsling.VarslingService
import no.nav.helse.inntektsmeldingsvarsel.RecurringJob
import java.time.Duration

class PollForVarslingsmeldingJob(
        private val kafkaProvider: ManglendeInntektsmeldingMeldingProvider,
        private val service: VarslingService,
        coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        waitTimeWhenEmptyQueue: Duration = Duration.ofSeconds(30)
) : RecurringJob(coroutineScope, waitTimeWhenEmptyQueue) {

    override fun doJob() {
        do {
            val wasEmpty = kafkaProvider
                    .getMessagesToProcess()
                    .onEach(service::handleMessage)
                    .isEmpty()

            if (!wasEmpty) {
                kafkaProvider.confirmProcessingDone()
            }
        } while (!wasEmpty)
    }
}