package commands

import bot
import dao.Dao
import dev.kord.core.behavior.interaction.*
import dev.kord.core.entity.interaction.SubCommand
import dev.kord.core.on
import dev.kord.rest.builder.interaction.*
import dev.kord.rest.builder.message.create.allowedMentions
import games.*
import guildId
import kotlinx.coroutines.*
import java.io.*
import java.net.URL
import javax.imageio.ImageIO
import kotlin.math.abs
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent as BICE
import dev.kord.core.event.interaction.GuildApplicationCommandInteractionCreateEvent as GACICE
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent as GCICICE
import dev.kord.core.event.interaction.GuildMessageCommandInteractionCreateEvent as GMCICE
import dev.kord.core.event.interaction.GuildUserCommandInteractionCreateEvent as GUCICE

// region Названия команд

private const val USER_INFO = "Информация"
private const val COMPARE = "Проверить на совместимость"
private const val RPS = "Сыграть в Камень-Ножницы-Бумага"
private const val REPEAT = "Повторить"
private const val LEADERBOARD = "leaderboard"
private const val PLAY = "play"

internal const val SOCIAL_CREDIT = "sc"
internal const val CAT_WIFE = "cw"

const val POSITIVE =
    "https://media.discordapp.net/attachments/777832858059407391/1008420638442135583/positive.png"
const val NEGATIVE =
    "https://media.discordapp.net/attachments/777832858059407391/1008420637926232084/negative.png"

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
            SOCIAL_CREDIT -> (this as GCICICE).socialCredit()
            LEADERBOARD -> (this as GCICICE).leaderboard()
            PLAY -> (this as GCICICE).play()
            CAT_WIFE -> (this as GCICICE).catWife()
        }
    }

    bot.on<BICE> {
        if (interaction.componentId.startsWith("2048")) handle2048buttons()
        if (interaction.componentId.startsWith("rps")) handleRpsButtons()
    }
}

// region Команды

suspend fun GUCICE.userInfo() {
    val user = interaction.target.asMember(guildId)
    val userRow = Dao.user(user.id.value) ?: Dao.addUser(user.id.value) ?: kotlin.run {
        interaction.respondEphemeral { content = "Ошибка в базе данных." }
        return
    }

    val image = withContext(Dispatchers.IO) {
        if (userRow.credit > 0) ImageIO.read(URL(POSITIVE)).apply {
            graphics.run {
                font = font.deriveFont(72f)
                drawString(userRow.credit.toString(), 500, 385)
                dispose()
            }
        } else ImageIO.read(URL(NEGATIVE)).apply {
            graphics.run {
                font = font.deriveFont(64f)
                drawString(userRow.credit.toString(), 280, 280)
                dispose()
            }
        }
    }
    val stream = ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }
    val content = ByteArrayInputStream(stream.toByteArray())

    interaction.respondEphemeral {
        allowedMentions()
        this.content = "${user.mention},"
        addFile("positive.png", content)
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
            Dao.allUsers()
                .sortedByDescending { it.credit }
                .take(10)
                .forEachIndexed { index, (id, credit) ->
                    appendLine("**${index + 1}**: <@$id> - $credit очков")
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