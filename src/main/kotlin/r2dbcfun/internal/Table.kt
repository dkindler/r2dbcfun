package r2dbcfun.internal

import r2dbcfun.util.toSnakeCase
import kotlin.reflect.KClass

class Table(val name: String) {
    constructor(kClass: KClass<*>) : this("${kClass.simpleName!!.toSnakeCase().toLowerCase()}s")
}
