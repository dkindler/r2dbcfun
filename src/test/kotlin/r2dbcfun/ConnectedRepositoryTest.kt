package r2dbcfun

import failfast.describe
import failfast.mock.mock
import failfast.mock.verify
import r2dbcfun.dbio.ConnectionProvider
import r2dbcfun.test.TestObjects.Entity
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isSameInstanceAs

object ConnectedRepositoryTest {
    val context = describe(ConnectedRepository::class) {
        val connection = mock<ConnectionProvider>()
        test("exposes Repository and Connection") {
            expectThat(ConnectedRepository.create<Entity>(connection)) {
                get { repository }.isA<Repository<Entity>>()
                get { this.connectionProvider }.isSameInstanceAs(connection)
            }
        }

        context("forwarding calls") {
            val repo = mock<Repository<Entity>>()
            val subject = ConnectedRepository(repo, connection)
            val entity = Entity()
            test("create call") {
                subject.create(entity)
                verify(repo) { create(connection, entity) }
            }
            test("update call") {
                subject.update(entity)
                verify(repo) { update(connection, entity) }
            }
            test("findById call") {
                data class MyPK(override val id: Long) : PK

                val id = MyPK(1)
                subject.findById(id)
                verify(repo) { findById(connection, id) }
            }
        }
    }
}
