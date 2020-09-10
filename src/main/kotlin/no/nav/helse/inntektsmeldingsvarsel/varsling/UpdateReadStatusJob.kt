package no.nav.helse.inntektsmeldingsvarsel.varsling

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.helse.inntektsmeldingsvarsel.ANTALL_LESTE_MELDINGER
import no.nav.helse.inntektsmeldingsvarsel.RecurringJob
import java.time.Duration

class UpdateReadStatusJob(
        private val service: VarslingService,
        private val receiptReader: ReadReceiptProvider,
        coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        waitTimeWhenEmptyQueue: Duration = Duration.ofHours(12)
) : RecurringJob(coroutineScope, waitTimeWhenEmptyQueue) {

    override fun doJob() {
        var isEmpty = false
        do {
            val varsler = service.finnUleste(5000)
            isEmpty = varsler.isEmpty()

            varsler.forEach {
                val isRead = receiptReader.isRead(it)
                if (isRead) {
                    service.oppdaterLestStatus(it, true)
                    ANTALL_LESTE_MELDINGER.inc()
                }
            }
        } while (!isEmpty)
    }

}