package no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository

import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.SpleisInntektsmeldingMelding
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource

class PostgresVentendeBehandlingerRepository(private val ds: DataSource) : VentendeBehandlingerRepository {

    private val logger = LoggerFactory.getLogger(PostgresVentendeBehandlingerRepository::class.java)
    private val tableName = "ventende_behandlinger"
    private val insertStatement = """
        INSERT INTO $tableName(fødselsnummer, organisasjonsnummer, fom, tom, opprettet) VALUES(?, ?, ?, ?, ?)
            ON CONFLICT ON CONSTRAINT unik_periode_constraint
            DO NOTHING
    """.trimIndent()

    private val removeStatement = "DELETE FROM $tableName where fødselsnummer = ? and organisasjonsnummer = ?"
    private val findStatement = "SELECT * FROM $tableName WHERE opprettet <= ?"

    override fun insertIfNotExists(fnr: String, virksomhet: String, fom: LocalDate, tom: LocalDate, opprettet: LocalDateTime) {
        ds.connection.use {
            it.prepareStatement(insertStatement).apply {
                setString(1, fnr)
                setString(2, virksomhet)
                setTimestamp(3, Timestamp.valueOf(fom.atStartOfDay()))
                setTimestamp(4, Timestamp.valueOf(tom.atStartOfDay()))
                setTimestamp(5, Timestamp.valueOf(opprettet))
            }.executeUpdate()
        }
    }

    override fun remove(fnr: String, virksomhet: String, con: Connection) {
        val affectedRows = con.prepareStatement(removeStatement).apply {
            setString(1, fnr)
            setString(2, virksomhet)
        }.executeUpdate()

        if (affectedRows == 0) {
            logger.info("Fikk melding om at IM ikke mangler, men ingen ventende")
        }
    }

    override fun findOlderThan(date: LocalDateTime): Set<SpleisInntektsmeldingMelding> {
        ds.connection.use {
            val resultList = HashSet<SpleisInntektsmeldingMelding>()
            val res = it.prepareStatement(findStatement).apply {
                setTimestamp(1, Timestamp.valueOf(date))
            }.executeQuery()
            while (res.next()) {
                resultList.add(mapToDto(res))
            }
            return resultList
        }
    }

    private fun mapToDto(res: ResultSet): SpleisInntektsmeldingMelding {
        return SpleisInntektsmeldingMelding(
            organisasjonsnummer = res.getString("organisasjonsnummer"),
            fødselsnummer = res.getString("fødselsnummer"),
            fom = res.getTimestamp("fom").toLocalDateTime().toLocalDate(),
            tom = res.getTimestamp("tom").toLocalDateTime().toLocalDate(),
            opprettet = res.getTimestamp("opprettet").toLocalDateTime(),
        )
    }
}
