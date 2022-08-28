package commands

import bot
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.interaction.SubCommand
import dev.kord.core.on
import dev.kord.rest.builder.interaction.int
import dev.kord.rest.builder.interaction.subCommand
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.allowedMentions
import dev.kord.rest.builder.message.create.embed
import games.*
import getUser
import guildId
import model.CatWife
import model.CatWives
import model.User
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.abs
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction as GCICI
import dev.kord.core.entity.interaction.GuildUserCommandInteraction as GUCI
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent as BICE
import dev.kord.core.event.interaction.GuildApplicationCommandInteractionCreateEvent as GACICE
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent as MSICE

// region Названия команд

private const val USER_INFO = "Информация"
private const val COMPARE = "Проверить на совместимость"
private const val RPS = "Сыграть в Камень-Ножницы-Бумага"
private const val LEADERBOARD = "leaderboard"
private const val PLAY = "play"

internal const val SOCIAL_CREDIT = "sc"
internal const val CAT_WIFE = "cw"

const val POSITIVE = "https://i.imgur.com/bdxSEjb.png"
const val NEGATIVE = "https://i.imgur.com/b5Q9fjS.png"

// endregion

suspend fun createCommands() {
    bot.createGuildApplicationCommands(guildId) {
        user(USER_INFO)
        user(COMPARE)
        user(RPS)
        input(
            name = LEADERBOARD,
            description = "Рейтинг социального кредита",
        )
        input(
            name = PLAY,
            description = "Игры",
        ) {
            subCommand(
                name = "2048",
                description = "Сыграть в 2048",
            ) {
                int(
                    name = "size",
                    description = "Размер поля",
                ) {
                    minValue = 4
                    maxValue = 8
                }
            }
            subCommand(
                name = "roulette",
                description = "Сыграть в Рулетку",
            ) {
                int(
                    name = "amount",
                    description = "Количество социального кредита на ставке",
                ) {
                    required = true
                }
                repeat(3) {
                    int(
                        name = "bet_$it",
                        description = "Ставка",
                    ) {
                        required = true
                        minValue = 0
                        maxValue = 36
                    }
                }
            }
        }
        socialCredit()
        catWife()
    }

    bot.on<GACICE> {
        when (interaction.invokedCommandName) {
            USER_INFO -> (interaction as GUCI).userInfo()
            COMPARE -> (interaction as GUCI).compare()
            LEADERBOARD -> (interaction as GCICI).leaderboard()
            PLAY -> (interaction as GCICI).play()
            RPS -> (interaction as GUCI).playRps()
            SOCIAL_CREDIT -> (interaction as GCICI).socialCredit()
            CAT_WIFE -> (interaction as GCICI).catWife()
        }
    }

    bot.on<BICE> {
        when (interaction.componentId.substringBefore('_')) {
            "2048" -> interaction.handle2048buttons()
            "rps" -> interaction.handleRpsButtons()
            "namecw" -> interaction.nameCatWife()
            "cw" -> interaction.viewCatWife()
            "sellcw" -> interaction.sellCatWife()
            "stealcw" -> interaction.stealCatWife()
        }
    }

    bot.on<MSICE> {
        when (interaction.modalId.substringBefore('_')) {
            "namecw" -> interaction.saveCatWife()
        }
    }
}

suspend fun GCICI.play() {
    val command = command as SubCommand
    when (command.name) {
        "2048" -> play2048(command.integers["size"]?.toInt() ?: 4)
        "roulette" -> playRoulette()
    }
}

// region Команды

suspend fun GUCI.userInfo() {
    val id = target.id.value
    val user = getUser(id)
    val catWives = transaction { CatWife.find { CatWives.ownerId eq id }.toList() }
    respondEphemeral {
        allowedMentions()
        embed {
            if (user.id.value == target.id.value) {
                title = if (user.credit > 0) "Партия гордится тобой!" else "Ну и ну! Вы разочаровали партию!"
                description = "На твоём счету **${user.credit}** социального кредита"
            } else {
                title = if (user.credit > 0) "Партия гордится этим гражданином!" else "Ну и ну! Какой позор!"
                description = "На счету ${target.mention} **${user.credit}** социального кредита"
            }
            thumbnail { url = if (user.credit > 0) POSITIVE else NEGATIVE }
            if (catWives.isNotEmpty())
                footer { text = "Кошко-жёны:" }
        }
        catWives.chunked(5).forEach { row ->
            actionRow {
                row.forEach {
                    interactionButton(
                        style = ButtonStyle.Secondary,
                        customId = "cw_${it.id.value}",
                    ) {
                        emoji = DiscordPartialEmoji(name = it.rarity.emoji)
                        label = it.name
                    }
                }
            }
        }
    }
}

suspend fun GUCI.compare() {
    val match = target.asUser()
    if (user.id == match.id) return run {
        respondEphemeral { content = "Хватит себя уже сравнивать!!!" }
    }
    val percent = 100 - abs(user.username.hashCode() - match.username.hashCode()) % 101
    respondPublic {
        allowedMentions()
        content = "${match.mention} подходит вам на **$percent%**."
    }
}

suspend fun GCICI.leaderboard() {
    var count = 0
    val (top, bottom) = transaction {
        val users = User.all()
            .sortedByDescending { it.credit }
        count = users.size
        users.take(10) to users.takeLast(10).asReversed()
    }
    respondEphemeral {
        allowedMentions()
        embed {
            title = "Рейтинг социального кредита"
            field {
                name = "Доска почёта"
                inline = true
                value = buildString {
                    top.forEachIndexed { index, (id, credit) ->
                        appendLine("**${index + 1}** - <@$id>: $credit очков")
                    }
                }
            }
            field {
                name = "Доска позора"
                inline = true
                value = buildString {
                    bottom.forEachIndexed { index, (id, credit) ->
                        appendLine("**${count - index}** - <@$id>: $credit очков")
                    }
                }
            }
        }
    }
}

// endregion