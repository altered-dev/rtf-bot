package model

import dev.kord.common.Color
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import kotlin.random.Random

object CatWives : LongIdTable() {
    val ownerId = reference("owner_id", Users.id)
    val name = varchar("name", 32).default("")
    val rarity = enumeration<CatWife.Rarity>("rarity")
    val imageUrl = varchar("image_url", 64)
}

class CatWife(id: EntityID<Long>) : LongEntity(id) {
    var ownerId by CatWives.ownerId
    var name by CatWives.name
    var rarity by CatWives.rarity
    var imageUrl by CatWives.imageUrl

    companion object : LongEntityClass<CatWife>(CatWives)

    enum class Rarity(
        val displayName: String,
        val color: Color? = null,
    ) {
        COMMON("Обычная"),
        UNCOMMON("Необычная", Color(0x73E5AC)),
        RARE("Редкая"),
        EPIC("Эпичная"),
        LEGENDARY("Легендарная"),
        DEV("Для разработчика");

        companion object : List<Rarity> by values().asList() {
            fun getRandom() = when (Random.nextDouble()) {
                in 0.0..0.6 -> COMMON
                in 0.6..0.8 -> UNCOMMON
                in 0.8..0.9 -> RARE
                in 0.9..0.95 -> EPIC
                else -> LEGENDARY
            }
        }
    }
}