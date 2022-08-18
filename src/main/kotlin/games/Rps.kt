package games

import bot
import dao.dao
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.allowedMentions
import guildId
import dev.kord.core.event.interaction.GlobalButtonInteractionCreateEvent as GBICE
import dev.kord.core.event.interaction.GuildButtonInteractionCreateEvent as GuBICE
import dev.kord.core.event.interaction.GuildUserCommandInteractionCreateEvent as GUCICE

class Rps {
    var player1Choice: Choice? = null
    var player2Choice: Choice? = null

    enum class Choice(val emoji: String) {
        ROCK("\uD83E\uDEA8"), PAPER("\uD83D\uDCC4"), SCISSORS("✂️");

        /**
         * TODO: алгоритм получше
         * @return -1 - победил первый игрок
         *
         *  0 - ничья
         *
         *  1 - победил второй игрок
         */
        infix fun vs(other: Choice): Int {
            return when (this) {
                ROCK -> when (other) {
                    ROCK -> 0
                    PAPER -> 1
                    SCISSORS -> -1
                }
                PAPER -> when (other) {
                    ROCK -> -1
                    PAPER -> 0
                    SCISSORS -> 1
                }
                SCISSORS -> when (other) {
                    ROCK -> 1
                    PAPER -> -1
                    SCISSORS -> 0
                }
            }
        }

        companion object : List<Choice> by values().asList() {
            fun findByName(name: String) = find { it.name.equals(name, true) }
        }
    }

    companion object {
        @JvmStatic
        val games = mutableMapOf<Pair<Snowflake, Snowflake>, Rps>()
    }
}

const val name = "Камень-Ножницы-Бумага!"
const val rpsWinCredits = 100

suspend fun GUCICE.playRps() {
    val player1 = interaction.user
    val player2 = interaction.target.asMember(guildId)
    if (player2.isBot) return run {
        interaction.respondEphemeral { content = "Мать свою позови!" }
    }
    if (player1.id == player2.id) return run {
        interaction.respondEphemeral { content = "Нельзя играть с собой!" }
    }
    if (player1.id to player2.id in Rps.games) return run {
        interaction.respondEphemeral { content = "У вас уже есть текущая игра!" }
    }
    Rps.games[player1.id to player2.id] = Rps()

    interaction.respondEphemeral {
        allowedMentions()
        content = "Вы вызвали ${player2.mention} в $name Выберите символ:"
        buttons(player2.id)
    }
    player2.getDmChannel().createMessage {
        content = "${player1.mention} вызвал в $name Выберите символ:"
        buttons(player1.id)
    }
}

// PLAYER 1
suspend fun GuBICE.handleRpsButtons() {
    val id = interaction.componentId
    val (choice, id2) = id.split('_')
    val player1 = interaction.user
    val player2 = bot.getUser(Snowflake(id2.toULong()))!!
    val game = Rps.games[player1.id to player2.id] ?: return run {
        interaction.respondEphemeral { content = "Игры не существует!" }
    }

    game.player1Choice = Rps.Choice.findByName(choice.substring(3))
    if (game.player2Choice != null) {
        val result = game.player1Choice!! vs game.player2Choice!!
        interaction.respondEphemeral {
            content = finalMessage(result, player2, game, false)
        }
        player2.getDmChannelOrNull()?.createMessage {
            content = finalMessage(-result, player1, game, true)
        }
        Rps.games -= player1.id to player2.id
        if (result != 0) addPointsToWinner(if (result == -1) player1.id.value else player2.id.value)
    } else interaction.respondEphemeral { content = "Выбор сделан." }
}

// PLAYER 2
suspend fun GBICE.handleRpsButtons() {
    val id = interaction.componentId
    val (choice, id1) = id.split('_')
    val player1 = bot.getUser(Snowflake(id1.toULong()))!!
    val player2 = interaction.user
    val game = Rps.games[player1.id to player2.id] ?: return run {
        interaction.respondEphemeral { content = "Игры не существует!" }
    }

    game.player2Choice = Rps.Choice.findByName(choice.substring(3))
    if (game.player1Choice != null) {
        val result = game.player1Choice!! vs game.player2Choice!!
        interaction.respondPublic {
            content = finalMessage(-result, player1, game, true)
        }
        player1.getDmChannelOrNull()?.createMessage {
            content = finalMessage(result, player2, game, false)
        }
        Rps.games -= player1.id to player2.id
        if (result != 0) addPointsToWinner(if (result == -1) player1.id.value else player2.id.value)
    } else interaction.respondEphemeral { content = "Выбор сделан." }
}

private suspend fun addPointsToWinner(id: ULong) {
    val user = dao.user(id) ?: dao.addUser(id) ?: return
    dao.editUser(id, user.credit + rpsWinCredits)
}

private fun finalMessage(
    result: Int,
    otherPlayer: User,
    game: Rps,
    isPlayer2: Boolean,
): String {
    val outcome = when (result) {
        -1 -> "выиграли"
        0 -> "получили ничью с"
        1 -> "програли"
        else -> "ебанулись"
    }
    return """
        Вы $outcome ${otherPlayer.mention} в $name!
        Ваш выбор - ${(if (isPlayer2) game.player2Choice else game.player1Choice)!!.emoji}
        Выбор противника - ${(if (isPlayer2) game.player1Choice else game.player2Choice)!!.emoji}
    """.trimIndent()
}

private fun MessageCreateBuilder.buttons(id: Snowflake) = actionRow {
    interactionButton(ButtonStyle.Secondary, "rpsrock_${id.value}") {
        emoji = DiscordPartialEmoji(name = "\uD83E\uDEA8")
    }
    interactionButton(ButtonStyle.Secondary, "rpspaper_${id.value}") {
        emoji = DiscordPartialEmoji(name = "\uD83D\uDCC4")
    }
    interactionButton(ButtonStyle.Secondary, "rpsscissors_${id.value}") {
        emoji = DiscordPartialEmoji(name = "✂️")
    }
}