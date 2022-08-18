package dao

import model.User

interface DAOFacade {
    suspend fun allUsers(): List<User>
    suspend fun user(id: ULong): User?
    suspend fun addUser(id: ULong): User?
    suspend fun editUser(id: ULong, credit: Long): Boolean
    suspend fun deleteUser(id: ULong): Boolean
}
