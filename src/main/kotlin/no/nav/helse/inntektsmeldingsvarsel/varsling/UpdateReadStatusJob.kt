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
    waitTimeWhenEmptyQueue: Duration = Duration.ofHours(1)
) : RecurringJob(coroutineScope, waitTimeWhenEmptyQueue) {

    override fun doJob() {
        service.finnSisteUleste(5000).forEach {
            val isRead = receiptReader.isRead(it)
            if (isRead) {
                service.oppdaterLestStatus(it, true)
                ANTALL_LESTE_MELDINGER.inc()
            }
        }
    }
}
