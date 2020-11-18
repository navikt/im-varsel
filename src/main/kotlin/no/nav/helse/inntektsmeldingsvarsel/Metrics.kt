package no.nav.helse.inntektsmeldingsvarsel

import io.prometheus.client.Counter

const val METRICS_NS = "helsearbeidsgiverimvarsel"

val ANTALL_INNKOMMENDE_MELDINGER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("inkommende_meldinger")
        .help("Teller antall innkommene meldinger om manglende IM")
        .register()

val ANTALL_FILTRERTE_VARSLER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("utfiltrerte_varsler")
        .help("Teller antall varsler som _ikke_ ble opprettet pga filter")
        .register()

val ANTALL_SENDTE_VARSLER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("sendte_varsler")
        .help("Teller antall meldinger om manglende IM sendt til virksomheter via Altinn")
        .register()

val ANTALL_SENDTE_BREV: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("sendte_brev")
        .help("Teller antall brev sendt via Altinn")
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
