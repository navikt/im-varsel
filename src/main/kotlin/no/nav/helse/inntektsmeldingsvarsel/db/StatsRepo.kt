package no.nav.helse.inntektsmeldingsvarsel.db
import javax.sql.DataSource
import kotlin.collections.ArrayList

data class VarselStats(
    val weekNumber: Int,
    val sent: Int,
    val lest: Int
)
interface IStatsRepo {
    fun getVarselStats(): List<VarselStats>
}

class StatsRepoImpl(
    private val ds: DataSource
) : IStatsRepo {
    override fun getVarselStats(): List<VarselStats> {
        val query = """
            SELECT s.year, s.uke, coalesce(lest, 0) as lest, sent FROM
              (SELECT extract('week' from date_trunc('week', lesttidspunkt)) as uke,
                      extract('year' from date_trunc('week', lesttidspunkt)) as year,
                      count(*) filter ( WHERE read = true and sent = true)   as lest
               from varsling
               where lesttidspunkt > NOW()::DATE - INTERVAL '12 MONTHS'
               group by extract('year' from date_trunc('week', lesttidspunkt)),
                        extract('week' from date_trunc('week', lesttidspunkt))
               order by extract('year' from date_trunc('week', lesttidspunkt)),
                        extract('week' from date_trunc('week', lesttidspunkt))
              ) as l
            full join
            (SELECT
                extract ('week' from date_trunc('week', behandlet)) as uke,
                extract ('year' from date_trunc('week', behandlet)) as year,
                count(*) filter ( WHERE sent = true) as sent
                from varsling
                where behandlet > NOW()::DATE - INTERVAL '12 MONTHS'
                group by extract ('year' from date_trunc('week', behandlet)), extract ('week' from date_trunc('week', behandlet))
                order by extract ('year' from date_trunc('week', behandlet)), extract ('week' from date_trunc('week', behandlet))
                ) as s on l.uke = s.uke and l.year = s.year;
        """.trimIndent()

        ds.connection.use {
            val res = it.prepareStatement(query).executeQuery()
            val returnValue = ArrayList<VarselStats>()
            while (res.next()) {
                returnValue.add(
                    VarselStats(
                        res.getInt("uke"),
                        res.getInt("sent"),
                        res.getInt("lest")
                    )
                )
            }
            return returnValue
        }
    }
}
