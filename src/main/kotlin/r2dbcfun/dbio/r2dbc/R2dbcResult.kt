package r2dbcfun.dbio.r2dbc

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import r2dbcfun.dbio.DBResult
import r2dbcfun.dbio.DBRow

class R2dbcResult(private val result: io.r2dbc.spi.Result) : DBResult {
    override suspend fun rowsUpdated(): Int = result.rowsUpdated.awaitSingle()

    override suspend fun <T : Any> map(mappingFunction: (t: DBRow) -> T): Flow<T> {
        return result.map { row, _ -> mappingFunction(R2dbcRow(row)) }.asFlow()
    }
}
