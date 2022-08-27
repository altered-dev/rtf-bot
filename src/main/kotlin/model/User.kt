@file:OptIn(ExperimentalUnsignedTypes::class)

package model

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable

object Users : IdTable<ULong>() {
    override val id = ulong("id").entityId()
    val credit = long("credit").default(0)
}

class User(id: EntityID<ULong>) : Entity<ULong>(id) {
    var credit by Users.credit

    operator fun component1() = id
    operator fun component2() = credit

    companion object : EntityClass<ULong, User>(Users)
}