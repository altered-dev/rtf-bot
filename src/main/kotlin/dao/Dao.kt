package dao

import dao.DatabaseFactory.dbQuery
import model.*
import org.jetbrains.exposed.sql.*

object Dao : DaoFacade {
    private fun resultRowToUser(row: ResultRow) = User(
        id = row[Users.id],
        credit = row[Users.credit],
    )

    private fun resultRowToCatWife(row: ResultRow) = CatWife(
        id = row[CatWives.id],
        ownerId = row[CatWives.ownerId],
        rarity = row[CatWives.rarity],
        name = row[CatWives.name],
        imageUrl = row[CatWives.imageUrl],
    )

    // region users
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
        Users.update({ Users.id eq id }) { user ->
            user[Users.credit] = credit
        } > 0
    }

    override suspend fun deleteUser(id: ULong): Boolean = dbQuery {
        Users.deleteWhere { Users.id eq id } > 0
    }
    // endregion

    // region cat wives
    override suspend fun allCatWives(): List<CatWife> = dbQuery {
        CatWives.selectAll().map(::resultRowToCatWife)
    }

    override suspend fun catWife(id: ULong): CatWife? = dbQuery {
        CatWives
            .select { CatWives.id eq id }
            .map(::resultRowToCatWife)
            .singleOrNull()
    }

    override suspend fun catWivesByOwner(id: ULong): List<CatWife> = dbQuery {
        CatWives
            .select { CatWives.ownerId eq id }
            .map(::resultRowToCatWife)
    }

    override suspend fun addCatWife(
        ownerId: ULong,
        rarity: CatWife.Rarity,
        name: String,
        imageUrl: String
    ): CatWife? = dbQuery {
        val insertStatement = CatWives.insert {
            it[CatWives.ownerId] = ownerId
            it[CatWives.rarity] = rarity
            it[CatWives.name] = name
            it[CatWives.imageUrl] = imageUrl
        }
        insertStatement.resultedValues?.singleOrNull()?.let(::resultRowToCatWife)
    }

    override suspend fun editCatWife(
        id: ULong,
        ownerId: ULong,
        rarity: CatWife.Rarity?,
        name: String?,
        imageUrl: String?
    ): Boolean = dbQuery {
        CatWives.update({ CatWives.id eq id }) { catWife ->
            rarity?.let { catWife[CatWives.rarity] = it }
            name?.let { catWife[CatWives.name] = it }
            imageUrl?.let { catWife[CatWives.imageUrl] = it }
        } > 0
    }

    override suspend fun deleteCatWife(id: ULong): Boolean = dbQuery {
        CatWives.deleteWhere { CatWives.id eq id } > 0
    }
    // endregion
}