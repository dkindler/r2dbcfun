package r2dbcfun.dbio.r2dbc

import io.r2dbc.spi.Clob
import io.r2dbc.spi.Row
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.asFlow
import r2dbcfun.dbio.DBRow
import r2dbcfun.dbio.LazyResult

class R2dbcRow(private val row: Row) : DBRow {
    override fun getLazy(key: String): LazyResult<Any?> {
        val value = row.get(key)
        return if (value is Clob) LazyResult { resolveClob(value) } else LazyResult { value }
    }

    override fun <T> get(key: String, type: Class<T>): T? = row.get(key, type)

    private suspend fun resolveClob(result: Clob): String {
        val sb = StringBuilder()
        result.stream()
            .asFlow()
            .collect { chunk -> @Suppress("BlockingMethodInNonBlockingContext") sb.append(chunk) }
        result.discard()
        return sb.toString()
    }

}
