package no.nav.helse.inntektsmeldingsvarsel

import io.prometheus.client.Counter
import io.prometheus.client.Summary

const val METRICS_NS = "helsearbeidsgiverimvarsel"

val ANTALL_INNKOMMENDE_MELDINGER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("inkommende_meldinger")
        .help("Teller antall innkommene meldinger om manglende IM")
        .register()

val ANTALL_DUPLIKATMELDINGER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("duplikatmeldiger")
        .help("Teller antall innkommene meldinger der perioden er sett før")
        .register()

val ANTALL_SENDTE_VARSLER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("sendte_varsler")
        .help("Teller antall meldinger om manglende IM sendt til virksomheter via Altinn")
        .register()
