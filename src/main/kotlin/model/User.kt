@file:OptIn(ExperimentalUnsignedTypes::class)

package model

import org.jetbrains.exposed.sql.Table

data class User(
    val id: ULong,
    val credit: Long = 0L,
)

object Users : Table() {
    val id = ulong("id")
    val credit = long("credit")

    override val primaryKey = PrimaryKey(id)
}
