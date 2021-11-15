package no.nav.helse.inntektsmeldingsvarsel.db
import java.util.Date
import javax.sql.DataSource
import kotlin.collections.ArrayList

data class VarselStats(
    val antall: Int,
    val dato: Date
)
interface IStatsRepo{
    fun getVarselStats(): List<VarselStats>
}

class StatsRepoImpl(
    private val ds: DataSource
): IStatsRepo {
    override fun getVarselStats(): List<VarselStats> {
        val query = """
            SELECT 
                count(*) as antall,
                date(data->>'opprettet') as dato
            FROM varsling
            WHERE sent = 1
            GROUP BY  dato;
        """.trimIndent()

        ds.connection.use {
            val res = it.prepareStatement(query).executeQuery()
            val returnValue = ArrayList<VarselStats>()
            while (res.next()){
                returnValue.add(
                    VarselStats(
                        res.getInt("antall"),
                        res.getDate("dato")
                    )
                )
            }
            return returnValue
        }
    }
}
