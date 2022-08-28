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
import games.handle2048buttons
import games.handleRpsButtons
import games.play2048
import games.playRps
import getUser
import guildId
import model.CatWife
import model.CatWives
import model.User
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.abs
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent as BICE
import dev.kord.core.event.interaction.GuildApplicationCommandInteractionCreateEvent as GACICE
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent as GCICICE
import dev.kord.core.event.interaction.GuildMessageCommandInteractionCreateEvent as GMCICE
import dev.kord.core.event.interaction.GuildUserCommandInteractionCreateEvent as GUCICE
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent as MSICE

// region Названия команд

private const val USER_INFO = "Информация"
private const val COMPARE = "Проверить на совместимость"
private const val RPS = "Сыграть в Камень-Ножницы-Бумага"
private const val REPEAT = "Повторить"
private const val LEADERBOARD = "leaderboard"
private const val PLAY = "play"

internal const val SOCIAL_CREDIT = "sc"
internal const val CAT_WIFE = "cw"

private const val POSITIVE = "https://i.imgur.com/bdxSEjb.png"
private const val NEGATIVE = "https://i.imgur.com/b5Q9fjS.png"

// endregion

suspend fun createCommands() {
    bot.createGuildApplicationCommands(guildId) {
        user(USER_INFO)
        user(COMPARE)
        user(RPS)
        message(REPEAT)
        input(LEADERBOARD, "Рейтинг социального кредита")
        input(PLAY, "Игры") {
            subCommand("2048", "Сыграть в 2048") {
                int("size", "Размер поля") {
                    minValue = 4
                    maxValue = 8
                }
            }
        }
        socialCredit()
        catWife()
    }

    bot.on<GACICE> {
        when (interaction.invokedCommandName) {
            USER_INFO -> (this as GUCICE).userInfo()
            COMPARE -> (this as GUCICE).compare()
            RPS -> (this as GUCICE).playRps()
            REPEAT -> (this as GMCICE).repeat()
            LEADERBOARD -> (this as GCICICE).leaderboard()
            PLAY -> (this as GCICICE).play()
            SOCIAL_CREDIT -> (this as GCICICE).socialCredit()
            CAT_WIFE -> (this as GCICICE).catWife()
        }
    }

    bot.on<BICE> {
        val id = interaction.componentId
        when {
            id.startsWith("2048") -> handle2048buttons()
            id.startsWith("rps") -> handleRpsButtons()
            id.startsWith("name_cat_wife") -> nameCatWife()
            id.startsWith("cw") -> viewCatWife()
            id.startsWith("sell_cw") -> sellCatWife()
        }
    }

    bot.on<MSICE> {
        val id = interaction.modalId
        when {
            id.startsWith("name_cat_wife") -> saveCatWife()
        }
    }
}

// region Команды

suspend fun GUCICE.userInfo() {
    val id = interaction.target.id.value
    val user = getUser(id)
    val catWives = transaction { CatWife.find { CatWives.ownerId eq id }.toList() }
    interaction.respondEphemeral {
        allowedMentions()
        this.content = "<@$id>,"
        embed {
            // TODO: messages for when another user is selected
            title = if (user.credit > 0) "Партия гордится тобой!" else "Ну и ну! Вы разочаровали партию!"
            description = "На твоём счету **${user.credit}** социального кредита"
            thumbnail { url = if (user.credit > 0) POSITIVE else NEGATIVE }
            if (catWives.isNotEmpty()) footer { text = "Кошко-жёны:" }
        }
        catWives.chunked(5).forEach { row ->
            actionRow {
                row.forEach {
                    interactionButton(ButtonStyle.Secondary, "cw_${it.id.value}") {
                        emoji = DiscordPartialEmoji(name = it.rarity.emoji)
                        label = it.name
                    }
                }
            }
        }
    }
}

suspend fun GUCICE.compare() {
    val user = interaction.user
    val match = interaction.target.asUser()
    if (user.id == match.id) return run {
        interaction.respondEphemeral { content = "Хватит себя уже сравнивать!!!" }
    }
    val percent = 100 - abs(user.username.hashCode() - match.username.hashCode()) % 101
    interaction.respondPublic {
        allowedMentions()
        content = "${match.mention} подходит вам на **$percent%**."
    }
}

suspend fun GMCICE.repeat() {
    interaction.respondPublic {
        allowedMentions()
        content = interaction.target.asMessage().content
    }
}

suspend fun GCICICE.leaderboard() {
    interaction.respondEphemeral {
        allowedMentions()
        content = buildString {
            appendLine("**Рейтинг социального кредита:**")
            transaction {
                User.all()
                    .sortedByDescending { it.credit }
                    .take(10)
                    .forEachIndexed { index, (id, credit) ->
                        appendLine("**${index + 1}**: <@$id> - $credit очков")
                    }
            }
        }
    }
}

suspend fun GCICICE.play() {
    val cmd = interaction.command as SubCommand

    when (cmd.name) {
        "2048" -> play2048(cmd.integers["size"]?.toInt() ?: 4)
    }
}

// endregion