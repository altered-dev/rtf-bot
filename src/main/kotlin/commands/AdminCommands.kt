package commands

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.interaction.SubCommand
import dev.kord.rest.builder.interaction.*
import dev.kord.rest.builder.message.create.allowedMentions
import model.CatWife
import model.User
import get
import getUser
import org.jetbrains.exposed.sql.transactions.transaction
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent as GCICICE

fun GuildMultiApplicationCommandBuilder.socialCredit() {
    input(
        name = SOCIAL_CREDIT,
        description = "Управление социальным кредитом",
    ) {
        defaultMemberPermissions = Permissions { +Permission.Administrator }
        subCommand(
            name = "balance",
            description = "Посмотреть баланс",
        ) {
            user(
                name = "user",
                description = "Пользователь",
            ) {
                required = true
            }
        }
        subCommand(
            name = "modify",
            description = "Изменить баланс относительно"
        ) {
            user(
                name = "user",
                description = "Пользователь",
            ) {
                required = true
            }
            int(
                name = "amount",
                description = "На какое количество изменить",
            ) {
                required = true
            }
        }
        subCommand(
            name = "set",
            description = "Задать баланс",
        ) {
            user(
                name = "user",
                description = "Пользователь",
            ) {
                required = true
            }
            int(
                name = "amount",
                description = "Какое количество задать",
            ) {
                required = true
            }
        }
    }
}

suspend fun GCICICE.socialCredit() {
    val cmd = interaction.command as SubCommand
    val user by cmd.users
    val amount = cmd.integers["amount"]
    val userRow = getUser(user.id.value)

    interaction.respondEphemeral {
        allowedMentions()
        when (cmd.name) {
            "balance" -> {
                content = "У ${user.mention} ${userRow.credit} очков."
            }
            "modify" -> {
                transaction { userRow.credit += amount!! }
                content = "Теперь у ${user.mention} ${userRow.credit + amount!!} очков."
            }
            "set" -> {
                transaction { userRow.credit = amount!! }
                content = "Теперь у ${user.mention} $amount очков."
            }
        }
    }
}


fun GuildMultiApplicationCommandBuilder.catWife() {
    input(
        name = CAT_WIFE,
        description = "Управление кошко-жёнами",
    ) {
        defaultMemberPermissions = Permissions { +Permission.Administrator }
        subCommand(
            name = "test",
            description = "Тестирование",
        )
        subCommand(
            name = "get",
            description = "Получить кошко-жену",
        ) {
            catWifeId()
        }
        subCommand(
            name = "add",
            description = "Добавить кошко-жену",
        ) {
            catWifeDescription(true)
        }
        subCommand(
            name = "edit",
            description = "Изменить кошко-жену",
        ) {
            catWifeId()
            catWifeDescription(false)
        }
        subCommand(
            name = "remove",
            description = "Удалить кошко-жену",
        ) {
            catWifeId()
        }
    }
}

private fun SubCommandBuilder.catWifeId() {
    int(
        name = "id",
        description = "ID кошко-жены",
    ) {
        required = true
        minValue = 0
        maxValue = ULong.MAX_VALUE.toLong()
    }
}

private fun SubCommandBuilder.catWifeDescription(isRequired: Boolean) {
    user(
        name = "owner",
        description = "Владелец",
    ) {
        required = isRequired
    }
    string(
        name = "rarity",
        description = "Редкость кошко-жены"
    ) {
        required = isRequired
        choice("Обычная", "common")
        choice("Редкая", "rare")
        choice("Эпическая", "epic")
        choice("Легендарная", "legendary")
        choice("Для разработчика", "dev")
    }
    string(
        name = "name",
        description = "Имя кошко-жены",
    ) {
        required = isRequired
    }
    string(
        name = "image_url",
        description = "Ссылка на картинку",
    ) {
        required = isRequired
    }
}

suspend fun GCICICE.catWife() {
    val cmd = interaction.command as SubCommand
    when (cmd.name) {
        "test" -> {
            giveCatWife()
        }
        "get" -> {
            val catWife = transaction { CatWife.findById(cmd.integers["id"]!!) } ?: return run {
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
