package commands

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.interaction.SubCommand
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.*
import dev.kord.rest.builder.message.create.allowedMentions
import model.CatWife
import model.User
import get
import getUser
import org.jetbrains.exposed.sql.transactions.transaction

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
    val userRow = getUser(user.id.value)

    interaction.respondEphemeral {
        allowedMentions()
        when (cmd.name) {
            "balance" -> content = "У <@${userRow.id}> ${userRow.credit} очков."
            "modify" -> {
                transaction { userRow.credit += amount!! }
                content = "Теперь у <@${userRow.id}> ${userRow.credit + amount!!} очков."
            }
            "set" -> {
                transaction { userRow.credit = amount!! }
                content = "Теперь у <@${userRow.id}> $amount очков."
            }
        }
    }
}


fun GuildMultiApplicationCommandBuilder.catWife() {
    input(CAT_WIFE, "Управление кошко-жёнами") {
        defaultMemberPermissions = Permissions { +Permission.Administrator }
        subCommand("test", "Тестирование")
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
    user("owner", "Владелец") {
        required = isRequired
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
        "test" -> {
            giveCatWife()
        }
        "get" -> {
            val catWife = transaction { CatWife.findById(cmd.integers["id"]!!) }
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
            transaction {
                CatWife.new {
                    ownerId = getUser(cmd.users["owner"]!!.id.value).id
                    rarity = CatWife.Rarity[cmd.strings["rarity"]!!]!!
                    name = cmd.strings["name"]!!
                    imageUrl = cmd.strings["image_url"]!!
                }
            }
            interaction.respondEphemeral { content = "Кошко-жена добавлена." }
        }
        "edit" -> {
            transaction {
                CatWife.findById(cmd.integers["id"]!!)?.apply {
                    // weird line of code warning
                    cmd.users["owner"]?.id?.value?.let { id -> User.findById(id)?.let { ownerId = it.id } }
                    cmd.strings["rarity"]?.let { rName -> CatWife.Rarity[rName]?.let { rarity = it } }
                    cmd.strings["name"]?.let { name = it }
                    cmd.strings["image_url"]?.let { imageUrl = it }
                }
            }
            interaction.respondEphemeral { content = "Кошко-жена изменена." }
        }
        "delete" -> {
            transaction { CatWife.findById(cmd.integers["id"]!!)?.delete() }
            interaction.respondEphemeral { content = "Кошко-жена удалена." }
        }
    }
}
