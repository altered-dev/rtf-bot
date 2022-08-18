package commands

import dao.Dao
import dev.kord.common.entity.*
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.interaction.SubCommand
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.*
import dev.kord.rest.builder.message.create.allowedMentions
import get
import model.CatWife

fun GuildMultiApplicationCommandBuilder.socialCredit() {
    input(SOCIAL_CREDIT, "Управление социальным кредитом") {
        defaultMemberPermissions = Permissions { +Permission.Administrator }
        subCommand("balance", "Посмотреть баланс") {
            user("user", "Пользователь") {
                required = true
            }
        }
        subCommand("modify", "Изменить баланс относительно") {
            user("user", "Пользователь") {
                required = true
            }
            int("amount", "На какое количество изменить") {
                required = true
            }
        }
        subCommand("set", "Задать баланс") {
            user("user", "Пользователь") {
                required = true
            }
            int("amount", "Какое количество задать") {
                required = true
            }
        }
    }
}

suspend fun GuildChatInputCommandInteractionCreateEvent.socialCredit() {
    val cmd = interaction.command as SubCommand
    val user by cmd.users
    val amount = cmd.integers["amount"]
    val userRow = Dao.user(user.id.value) ?: Dao.addUser(user.id.value) ?: return run {
        interaction.respondEphemeral { content = "Ошибка в базе данных." }
    }

    interaction.respondEphemeral {
        allowedMentions()
        when (cmd.name) {
            "balance" -> content = "У <@${userRow.id}> ${userRow.credit} очков."
            "modify" -> {
                Dao.editUser(userRow.id, userRow.credit + amount!!)
                content = "Теперь у <@${userRow.id}> ${userRow.credit + amount} очков."
            }
            "set" -> {
                Dao.editUser(userRow.id, amount!!)
                content = "Теперь у <@${userRow.id}> $amount очков."
            }
        }
    }
}

fun GuildMultiApplicationCommandBuilder.catWife() {
    input(CAT_WIFE, "Управление кошко-жёнами") {
        defaultMemberPermissions = Permissions { +Permission.Administrator }
        subCommand("get", "Получить кошко-жену") {
            catWifeId()
        }
        subCommand("add", "Добавить кошко-жену") {
            catWifeDescription(true)
        }
        subCommand("edit", "Изменить кошко-жену") {
            catWifeId()
            catWifeDescription(false)
        }
        subCommand("remove", "Удалить кошко-жену") {
            catWifeId()
        }
    }
}

private fun SubCommandBuilder.catWifeId() {
    int("id", "ID кошко-жены") {
        required = true
        minValue = 0
        maxValue = ULong.MAX_VALUE.toLong()
    }
}

private fun SubCommandBuilder.catWifeDescription(isRequired: Boolean) {
    int("owner_id", "ID владельца") {
        required = isRequired
        minValue = 0
        maxValue = ULong.MAX_VALUE.toLong()
    }
    string("rarity", "Редкость кошко-жены") {
        required = isRequired
        choice("Обычная", "common")
        choice("Редкая", "rare")
        choice("Эпическая", "epic")
        choice("Легендарная", "legendary")
        choice("Для разработчика", "dev")
    }
    string("name", "Имя кошко-жены") {
        required = isRequired
    }
    string("image_url", "Ссылка на картинку") {
        required = isRequired
    }
}

suspend fun GuildChatInputCommandInteractionCreateEvent.catWife() {
    val cmd = interaction.command as SubCommand
    when (cmd.name) {
        "get" -> {
            val catWife = Dao.catWife(cmd.integers["id"]!!.toULong())
                ?: return run {
                    interaction.respondEphemeral { content = "Кошко-жены с таким ID не найдено." }
                }
            interaction.respondEphemeral {
                allowedMentions()
                content = """
                    Кошко-жена: **${catWife.name}**
                    Редкость: **${catWife.rarity.displayName}**
                    Ссылка на картинку: **${catWife.imageUrl}**
                    """.trimIndent()
            }
        }
        "add" -> {
            Dao.addCatWife(
                cmd.integers["owner_id"]!!.toULong(),
                CatWife.Rarity[cmd.strings["rarity"]!!]!!,
                cmd.strings["name"]!!,
                cmd.strings["image_url"]!!,
            )
            interaction.respondEphemeral { content = "Кошко-жена добавлена." }
        }
        "edit" -> {
            Dao.editCatWife(
                cmd.integers["owner_id"]!!.toULong(),
                cmd.integers["id"]!!.toULong(),
                cmd.strings["rarity"]?.let { CatWife.Rarity[it] },
                cmd.strings["name"],
                cmd.strings["image_url"],
            )
            interaction.respondEphemeral { content = "Кошко-жена изменена." }
        }
        "delete" -> {
            Dao.deleteCatWife(cmd.integers["id"]!!.toULong())
            interaction.respondEphemeral { content = "Кошко-жена удалена." }
        }
    }
}
