package r2dbcfun.internal

import io.vertx.sqlclient.data.Numeric
import r2dbcfun.RepositoryException
import r2dbcfun.util.toSnakeCase
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaType

// from the r2dbc spec: https://r2dbc.io/spec/0.8.1.RELEASE/spec/html/#datatypes
private val passThroughFieldConverter = FieldConverter { it }
private val fieldConverters =
    mapOf<KClass<*>, FieldConverter>(
        String::class to passThroughFieldConverter,
        io.r2dbc.spi.Clob::class to passThroughFieldConverter,
        Boolean::class to passThroughFieldConverter,
        java.nio.ByteBuffer::class to passThroughFieldConverter,
        io.r2dbc.spi.Blob::class to passThroughFieldConverter,
        Int::class to passThroughFieldConverter,
        Byte::class to passThroughFieldConverter,
        Short::class to passThroughFieldConverter,
        Long::class to passThroughFieldConverter,
        Double::class to FieldConverter { (it as Number?)?.toDouble() },
        BigDecimal::class to FieldConverter { if (it is Numeric) it.bigDecimalValue() else it },
        LocalDate::class to passThroughFieldConverter
    )


internal class ClassCreator<T : Any>(
    kClass: KClass<T>,
    private val idHandler: IDHandler<T>,
    val otherClasses: Set<KClass<*>>
) {
    val constructor: KFunction<T> =
        kClass.primaryConstructor
            ?: throw RuntimeException("No primary constructor found for ${kClass.simpleName}")

    val fieldInfo = constructor.parameters.map { parameter ->
        FieldInfo(
            parameter,
            parameter.name!!.toSnakeCase(),
            makeConverter(parameter)
        )
    }

    private fun makeConverter(parameter: KParameter): FieldConverter {
        val type = parameter.type
        val javaClass = type.javaType as Class<*>
        val kotlinClass = type.classifier as KClass<*>
        if (otherClasses.contains(kotlinClass))
            return BelongsToConverter(kotlinClass)
        return when {
            javaClass.isEnum -> EnumConverter(javaClass)
            else -> {
                val isPK = parameter.name == "id"
                if (isPK) FieldConverter { idHandler.createId(it as Long) }
                else
                    fieldConverters[kotlinClass]
                        ?: throw RepositoryException("type ${kotlinClass.simpleName} not supported")
            }
        }
    }

    data class FieldInfo(
        val constructorParameter: KParameter,
        val snakeCaseName: String,
        val fieldConverter: FieldConverter
    )
}

class BelongsToConverter(kotlinClass: KClass<*>) : FieldConverter {
    override fun valueToConstructorParameter(value: Any?): Any? {
        TODO("Not yet implemented")
    }

}


/** converts strings from the database to enums in the mapped class */
private class EnumConverter(private val clazz: Class<*>) : FieldConverter {
    override fun valueToConstructorParameter(value: Any?): Any? {
        if (value == null) return null

        @Suppress("UPPER_BOUND_VIOLATED", "UNCHECKED_CAST", "RemoveExplicitTypeArguments")
        return (java.lang.Enum.valueOf<Any>(clazz as Class<Any>, value as String))
    }
}

internal fun interface FieldConverter {
    fun valueToConstructorParameter(value: Any?): Any?
}