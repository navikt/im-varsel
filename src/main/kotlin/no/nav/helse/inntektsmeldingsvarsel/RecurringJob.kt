package no.nav.helse.inntektsmeldingsvarsel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import org.slf4j.LoggerFactory
import java.time.Duration

abstract class RecurringJob(
    private val coroutineScope: CoroutineScope,
    private val waitTimeBetweenRuns: Duration
) {

    protected val logger = LoggerFactory.getLogger(this::class.java)

    private var isRunning = false

    fun startAsync(retryOnFail: Boolean = false) {
        logger.debug("Starter opp")
        isRunning = true
        scheduleAsyncJobRun(retryOnFail)
    }

    private fun scheduleAsyncJobRun(retryOnFail: Boolean) {
        coroutineScope.launch {
            try {
                doJob()
            } catch (t: Throwable) {
                if (retryOnFail)
                    logger.error("Jobben feilet, men forsøker på nytt etter ${waitTimeBetweenRuns.toSeconds()} s ", t)
                else {
                    isRunning = false
                    throw t
                }
            }

            if (isRunning) {
                delay(waitTimeBetweenRuns)
                if (isRunning) {
                    scheduleAsyncJobRun(retryOnFail)
                }
            } else {
                logger.debug("Stoppet.")
            }
        }
    }

    fun stop() {
        logger.debug("Stopper jobben...")
        isRunning = false
    }

    abstract fun doJob()
}
