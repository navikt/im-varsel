package no.nav.helse.inntektsmeldingsvarsel

import io.prometheus.client.Counter

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

val ANTALL_PERSONER_I_SENDTE_VARSLER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("personer_i_sendte_varsler")
        .help("Teller personer som det har blitt sendt varsel for")
        .register()

val ANTALL_LESTE_MELDINGER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("antall_leste_meldinger")
        .help("Teller antall meldinger i Altinn som er lest")
        .register()
