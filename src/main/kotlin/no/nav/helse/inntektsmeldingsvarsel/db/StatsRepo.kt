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
            SELECT
                extract('week' from date_trunc('week',behandlet)) as uke,
                extract('year' from date_trunc('week',behandlet)) as year,
                count(*) filter( WHERE sent = true) as sent,
                count(*) filter( WHERE read=true and sent = true) as lest
            from varsling
            where behandlet > NOW()::DATE - INTERVAL '12 MONTHS'
            group by  extract('year' from date_trunc('week',behandlet)), extract('week' from date_trunc('week',behandlet))
            order by  extract('year' from date_trunc('week',behandlet)), extract('week' from date_trunc('week',behandlet));
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
