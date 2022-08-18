package dao

import model.*

interface DaoFacade {
    suspend fun allUsers(): List<User>
    suspend fun user(id: ULong): User?
    suspend fun addUser(id: ULong): User?
    suspend fun editUser(id: ULong, credit: Long): Boolean
    suspend fun deleteUser(id: ULong): Boolean

    suspend fun allCatWives(): List<CatWife>
    suspend fun catWife(id: ULong): CatWife?
    suspend fun catWivesByOwner(id: ULong): List<CatWife>
    suspend fun addCatWife(
        ownerId: ULong,
        rarity: CatWife.Rarity,
        name: String,
        imageUrl: String
    ): CatWife?
    suspend fun editCatWife(
        id: ULong,
        ownerId: ULong,
        rarity: CatWife.Rarity? = null,
        name: String? = null,
        imageUrl: String? = null
    ): Boolean
    suspend fun deleteCatWife(id: ULong): Boolean
}
