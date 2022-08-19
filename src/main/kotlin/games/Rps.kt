package games

import bot
import dev.kord.common.entity.*
import dev.kord.core.behavior.channel.*
import dev.kord.core.behavior.interaction.*
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.create.*
import get
import guildId
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent as BICE
import dev.kord.core.event.interaction.GuildUserCommandInteractionCreateEvent as GUCICE
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

suspend fun GUCICE.playRps() {
    val player1 = interaction.user
    val player2 = interaction.target.asMember(guildId)
    if (player2.isBot) return run {
        interaction.respondEphemeral { content = "Мать свою позови!" }
    }
    if (player1.id == player2.id) return run {
        interaction.respondEphemeral { content = "Нельзя играть с собой!" }
    }
    if (player1.id to player2.id in Rps.games || player2.id to player1.id in Rps.games) return run {
        interaction.respondEphemeral { content = "У вас уже есть текущая игра!" }
    }
    Rps.games[player1.id to player2.id] = Rps()

    interaction.respondEphemeral {
        allowedMentions()
        content = "Вы вызвали ${player2.mention} в $name! Выберите символ:"
        buttons(player2.id, false)
    }
    player2.getDmChannel().createMessage {
        content = "${player1.mention} вызвал вас в $name! Выберите символ:"
        buttons(player1.id, false)
    }
}

private fun MCB.buttons(id: Snowflake, disabled: Boolean = false, chosen: Rps.Choice? = null) {
    for (row in Rps.Choice.chunked(5)) actionRow {
        for (choice in row) interactionButton(
            if (choice == chosen) ButtonStyle.Success else ButtonStyle.Secondary,
            "rps${choice.name.lowercase()}_${id.value}"
        ) {
            emoji = DiscordPartialEmoji(name = choice.emoji)
            this.disabled = disabled
        }
    }
}

suspend fun BICE.handleRpsButtons() {
    val (choiceName, otherId) = interaction.componentId.split('_')
    val user =  interaction.user
    val other = bot.getUser(Snowflake(otherId))!!
    val game = Rps.games[user.id to other.id]
        ?: Rps.games[other.id to user.id]
        ?: Rps().also { Rps.games[user.id to other.id] = it }

    val choice = Rps.Choice[choiceName.substring(3)]!!
    interaction.updateEphemeralMessage {
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