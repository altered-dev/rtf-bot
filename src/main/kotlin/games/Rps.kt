package games

import bot
import dev.kord.common.entity.*
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.updateEphemeralMessage
import dev.kord.core.entity.User
import dev.kord.core.entity.interaction.ButtonInteraction
import dev.kord.core.entity.interaction.GuildUserCommandInteraction
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.allowedMentions
import get
import dev.kord.rest.builder.message.create.MessageCreateBuilder as MCB

class Rps(var firstChoice: Choice? = null) {
    enum class Choice(val emoji: String) {
        ROCK("\uD83E\uDEA8"), GUN("\uD83D\uDD2B"), LIGHTNING("⚡"), DEVIL("\uD83D\uDE08"), DRAGON("\uD83D\uDC09"),
        WATER("\uD83D\uDCA6"), AIR("\uD83D\uDCA8"), PAPER("\uD83D\uDCC4"), SPONGE("\uD83E\uDDFD"), WOLF("\uD83D\uDC3A"),
        TREE("\uD83C\uDF33"), HUMAN("\uD83E\uDDCD"), SNAKE("\uD83D\uDC0D"), SCISSORS("\u2702\uFE0F"), FIRE("\uD83D\uDD25");

        infix fun vs(other: Choice) = when {
            ordinal == other.ordinal -> Outcome.DRAW
            (other.ordinal - ordinal).mod(size) > size / 2 -> Outcome.WIN
            else -> Outcome.LOSE
        }

        companion object : List<Choice> by values().asList()
    }

    enum class Outcome(val displayName: String) {
        WIN("выиграли"), LOSE("проиграли"), DRAW("получили ничью");

        operator fun unaryMinus() = when (this) {
            WIN -> LOSE
            LOSE -> WIN
            DRAW -> DRAW
        }
    }

    companion object {
        @JvmStatic
        val games = mutableMapOf<Pair<Snowflake, Snowflake>, Rps>()
    }
}

const val name = "Камень-Ножницы-Бумага"

suspend fun GuildUserCommandInteraction.playRps() {
    val target = target.asMember(guildId)
    when {
        target.isBot -> respondEphemeral { content = "Мать свою позови!" }
        user.id == target.id -> respondEphemeral { content = "Нельзя играть с собой!" }
        user.id to target.id in Rps.games || target.id to user.id in Rps.games ->
            respondEphemeral { content = "У вас уже есть текущая игра!" }
        else -> {
            Rps.games[user.id to target.id] = Rps()
            respondEphemeral {
                allowedMentions()
                content = "Вы вызвали ${target.mention} в $name! Выберите символ:"
                buttons(target.id)
            }
            target.getDmChannel().createMessage {
                content = "${user.mention} вызвал вас в $name! Выберите символ:"
                buttons(user.id)
            }
        }
    }
}

private fun MCB.buttons(id: Snowflake, disabled: Boolean = false, chosen: Rps.Choice? = null) {
    for (row in Rps.Choice.chunked(5)) actionRow {
        for (choice in row) interactionButton(
            if (choice == chosen) ButtonStyle.Success else ButtonStyle.Secondary,
            "rps_${choice.name.lowercase()}_${id.value}"
        ) {
            emoji = DiscordPartialEmoji(name = choice.emoji)
            this.disabled = disabled
        }
    }
}

suspend fun ButtonInteraction.handleRpsButtons() {
    val (_, choiceName, otherId) = componentId.split('_')
    val other = bot.getUser(Snowflake(otherId))!!
    val game = Rps.games[user.id to other.id]
        ?: Rps.games[other.id to user.id]
        ?: Rps().also { Rps.games[user.id to other.id] = it }

    val choice = Rps.Choice[choiceName]!!
    updateEphemeralMessage {
        allowedMentions()
        content = "Выбор против ${other.mention} сделан."
        buttons(other.id, true, choice)
    }
    if (game.firstChoice == null) {
        game.firstChoice = choice
        return
    }
    val outcome = choice vs game.firstChoice!!
    sendFinalMessage(user, other, outcome, choice, game.firstChoice!!)
    sendFinalMessage(other, user, -outcome, game.firstChoice!!, choice)
    Rps.games -= user.id to other.id
    Rps.games -= other.id to user.id
}

suspend fun sendFinalMessage(
    user: User,
    other: User,
    outcome: Rps.Outcome,
    choice: Rps.Choice,
    otherChoice: Rps.Choice
) = user.getDmChannelOrNull()?.createEmbed {
    title = "Вы ${outcome.displayName} в $name!"
    description = "против ${other.mention}"
    field {
        name = "Ваш выбор"
        value = choice.emoji
        inline = true
    }
    field {
        name = "Выбор противника"
        value = otherChoice.emoji
        inline = true
    }
}