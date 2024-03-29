package r2dbcfun

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import r2dbcfun.dbio.DBResult
import r2dbcfun.dbio.LazyResult
import r2dbcfun.internal.ClassInfo

interface ResultMapper<T : Any> {
    suspend fun mapQueryResult(queryResult: DBResult): Flow<T>
}

internal class ResultMapperImpl<T : Any>(
    private val classInfo: ClassInfo<T>
) : ResultMapper<T> {

    override suspend fun mapQueryResult(queryResult: DBResult): Flow<T> {
        data class ResultPair(val fieldInfo: ClassInfo.FieldInfo, val result: LazyResult<Any?>)

        val parameters: Flow<List<ResultPair>> =
            queryResult
                .map { row ->
                    classInfo.fieldInfo
                        .map { entry ->
                            val result = row.getLazy(entry.dbFieldName)
                            ResultPair(entry, result)
                        }
                }
        return parameters.map { values ->
            val resolvedParameters =
                values.associateTo(HashMap()) { (fieldInfo, result) ->
                    val resolvedValue = result.resolve()
                    val value = fieldInfo.fieldConverter.dbValueToParameter(resolvedValue)
                    Pair(fieldInfo.constructorParameter, value)
                }
            try {
                classInfo.constructor.callBy(resolvedParameters)
            } catch (e: IllegalArgumentException) {
                throw RepositoryException(
                    "error invoking constructor for ${classInfo.name}. parameters:$resolvedParameters",
                    e
                )
            }
        }
    }
}
