package r2dbcfun.query

import failfast.describe
import failfast.mock.mock
import r2dbcfun.ResultMapper
import r2dbcfun.dbio.ConnectionProvider
import r2dbcfun.internal.IDHandler
import r2dbcfun.internal.Table
import r2dbcfun.test.TestObjects.Entity

object QueryFactoryTest {
    val context = describe(QueryFactory::class) {
        val resultMapper = mock<ResultMapper<Entity>>()
        val queryFactory =
            QueryFactory(Table("table"), Entity::class, resultMapper, mock(), IDHandler(Entity::class), mock())
        val connection = mock<ConnectionProvider>()
        val condition = Entity::id.isEqualTo()
        test("can create query with one parameter") {
            val query = queryFactory.createQuery(condition)
            query.with(connection, 1)
        }
        test("can create query with two parameter") {
            val query = queryFactory.createQuery(condition, condition)
            query.with(connection, 1, 1)
        }
        test("can create query with three parameter") {
            val query = queryFactory.createQuery(condition, condition, condition)
            query.with(connection, 1, 1, 1)
        }
    }

}
