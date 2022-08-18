@file:OptIn(ExperimentalUnsignedTypes::class)

package model

import org.jetbrains.exposed.sql.Table

data class CatWife(
    val id: ULong,
    val ownerId: ULong,
    val rarity: Rarity,
    val name: String,
    val imageUrl: String,
) {
    enum class Rarity(
        val displayName: String,
    ) {
        COMMON("Обычная"),
        UNCOMMON("Необычная"),
        RARE("Редкая"),
        EPIC("Эпичная"),
        LEGENDARY("Легендарная"),
        DEV("Для разработчика");

        companion object : List<Rarity> by values().asList()
    }
}

object CatWives : Table() {
    val id = ulong("id").autoIncrement()
    val ownerId = ulong("owner_id")
    val rarity = enumeration<CatWife.Rarity>("rarity")
    val name = varchar("name", 32)
    val imageUrl = varchar("image_url", 64)

    override val primaryKey = PrimaryKey(id)
}