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

    val cost get() = rarity.baseCost + name.hashCode().mod(101) - 50

    companion object : LongEntityClass<CatWife>(CatWives)

    enum class Rarity(
        val displayName: String,
        val emoji: String,
        val baseCost: Int,
        val color: Color? = null,
    ) {
        COMMON("Обычная", "\uD83D\uDC31", 100),
        UNCOMMON("Необычная", "\uD83D\uDE40", 250, Color(0x73E5AC)),
        RARE("Редкая", "\uD83D\uDC3C", 500, Color(0xFFFF80)),
        EPIC("Эпичная", "✨", 1200, Color(0xFFBF80)),
        LEGENDARY("Легендарная", "\uD83D\uDC51", 3000, Color(0x80D4FF)),
        DEV("Для разработчика", "\uD83E\uDDD1\u200D\uD83D\uDCBB", 999999999);

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