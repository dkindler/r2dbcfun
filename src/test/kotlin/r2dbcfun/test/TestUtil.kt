package r2dbcfun.test

import io.kotest.core.TestConfiguration
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContextScope
import io.kotest.inspectors.forAll
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager
import java.util.*


fun prepareH2(): ConnectionFactory {
    val uuid = UUID.randomUUID()
    val databaseName = "r2dbc-test$uuid"
    val jdbcUrl = "jdbc:h2:mem:$databaseName;DB_CLOSE_DELAY=-1"
    val flyway = Flyway.configure().dataSource(jdbcUrl, "", "").load()
    flyway.migrate()
    return ConnectionFactories.get("r2dbc:h2:mem:///$databaseName;DB_CLOSE_DELAY=-1")
}

val container: PostgreSQLContainer<Nothing> by
    lazy {
        PostgreSQLContainer<Nothing>("postgres:13").apply {
// WIP           setCommand("postgres", "-c", "fsync=off", "-c", "max_connections=200")
            withReuse(true)
            start()
        }
    }

fun preparePostgreSQL(): ConnectionFactory {
    Class.forName("org.postgresql.Driver")
    val uuid = UUID.randomUUID()
    val databaseName = "r2dbctest$uuid".replace("-", "_")
    val host = container.containerIpAddress
    val port = container.getMappedPort(5432)
    val db =
        DriverManager.getConnection("jdbc:postgresql://$host:$port/postgres", "test", "test")
    db.createStatement().executeUpdate("create database $databaseName")
    db.close()

    val flyway =
        Flyway.configure()
            .dataSource("jdbc:postgresql://$host:$port/$databaseName", "test", "test")
            .load()
    flyway.migrate()
    return ConnectionFactories.get("r2dbc:pool:postgresql://test:test@$host:$port/$databaseName?initialSize=1")
}

data class Database(val name: String, val makeConnectionFactory: () -> ConnectionFactory)
val databases = listOf(Database("h2") { prepareH2() }, Database("psql") { preparePostgreSQL() })

fun forAllDatabases(
    funSpec: FunSpec,
    testName: String,
    tests: suspend FunSpecContextScope.(ConnectionFactory) -> Unit
) {
    databases.forAll { db ->
        funSpec.context("$testName on ${db.name}") {
            val connectionFactory = db.makeConnectionFactory()
            tests(connectionFactory)
        }
    }
}

fun <T : Any> TestConfiguration.autoClose(wrapped: T, closeFunction: (T) -> Unit): T {
    autoClose(object : AutoCloseable {
        override fun close() {
            closeFunction(wrapped)
        }
    })
    return wrapped
}