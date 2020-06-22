package no.nav.helse.inntektsmeldingsvarsel.varsling

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.helse.inntektsmeldingsvarsel.RecurringJob
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class SendVarslingJob(
        private val service: VarslingService,
        private val sender: VarslingSender,
        coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        waitTimeWhenEmptyQueue: Duration = Duration.ofHours(1)
) : RecurringJob(coroutineScope, waitTimeWhenEmptyQueue) {
    val periodeStrategy: VarslingsPeriodeStrategy = DailyVarslingStrategy()

    override fun doJob() {
        val now = LocalDateTime.now()
        if (now.hour < 7 || now.hour > 19) {
            return
        }

        var isEmpty = false
        val prevPeriod = periodeStrategy.previousPeriodeId(LocalDate.now())
        do {
            val varslinger = service.finnNesteUbehandlede(100, prevPeriod)
            isEmpty = varslinger.isEmpty()
            varslinger.forEach {
                sender.send(it)
                service.oppdaterSendtStatus(it, true)
            }
        } while (!isEmpty)
    }

}
