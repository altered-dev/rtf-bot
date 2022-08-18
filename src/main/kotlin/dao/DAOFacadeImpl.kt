package dao

import dao.DatabaseFactory.dbQuery
import model.User
import model.Users
import org.jetbrains.exposed.sql.*

class DAOFacadeImpl : DAOFacade {
    private fun resultRowToUser(row: ResultRow) = User(
        id = row[Users.id],
        credit = row[Users.credit],
    )

    override suspend fun allUsers(): List<User> = dbQuery {
        Users.selectAll().map(::resultRowToUser)
    }

    override suspend fun user(id: ULong): User? = dbQuery {
        Users
            .select { Users.id eq id }
            .map(::resultRowToUser)
            .singleOrNull()
    }

    override suspend fun addUser(id: ULong): User? = dbQuery {
        val insertStatement = Users.insert {
            it[Users.id] = id
            it[credit] = 0L
        }
        insertStatement.resultedValues?.singleOrNull()?.let(::resultRowToUser)
    }

    override suspend fun editUser(id: ULong, credit: Long): Boolean = dbQuery {
        Users.update({ Users.id eq id }) {
            it[Users.credit] = credit
        } > 0
    }

    override suspend fun deleteUser(id: ULong): Boolean = dbQuery {
        Users.deleteWhere { Users.id eq id } > 0
    }
}

val dao: DAOFacade = DAOFacadeImpl()