package games

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.updateEphemeralMessage
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import getUser
import kotlin.random.Random
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent as GCICICE

class Game2048(val size: Int) {
    var score = 0
    private var field = List(size) { IntArray(size) }
    private val bounds inline get() = 0 until size

    init {
        addTile()
        addTile()
    }

    private fun addTile() {
        var x: Int
        var y: Int
        do {
            x = Random.nextInt(size)
            y = Random.nextInt(size)
        } while (field[x][y] != 0)
        field[x][y] = if (Random.nextDouble() > 0.7) 4 else 2
    }

    val isGameOver: Boolean get() {
        repeat(size) { x ->
            repeat(size - 1) { y ->
                if (this.field[x][y] == 0 ||
                    this.field[x][y] == this.field[x][y + 1] ||
                    this.field[y][x] == this.field[y + 1][x])
                    return false
            }
            if (this.field[x][size - 1] == 0)
                return false
        }
        return true
    }

    fun moveHorizontally(dx: Int) {
        val newField = List(size) { IntArray(size) }
        for (y in bounds) {
            var x = if (dx == 1) size - 1 else 0
            var ix = x
            while (x in bounds && ix in bounds) {
                if (field[ix][y] == 0) {
                    ix -= dx
                    continue
                }
                newField[x][y] = field[ix][y]
                do ix -= dx while (ix in bounds && field[ix][y] == 0)
                if (ix in bounds && field[ix][y] == newField[x][y]) {
                    newField[x][y] *= 2
                    score += newField[x][y]
                    ix -= dx
                }
                x -= dx
            }
        }
        val moved = moved(newField)
        field = newField
        if (moved) addTile()
    }

    fun moveVertically(dy: Int) {
        val newField = List(size) { IntArray(size) }
        for (x in bounds) {
            var y = if (dy == 1) size - 1 else 0
            var iy = y
            while (y in bounds && iy in bounds) {
                if (field[x][iy] == 0) {
                    iy -= dy
                    continue
                }
                newField[x][y] = field[x][iy]
                do iy -= dy while (iy in bounds && field[x][iy] == 0)
                if (iy in bounds && field[x][iy] == newField[x][y]) {
                    newField[x][y] *= 2
                    score += newField[x][y]
                    iy -= dy
                }
                y -= dy
            }
        }
        val moved = moved(newField)
        field = newField
        if (moved) addTile()
    }

    private fun moved(newField: List<IntArray>) = bounds.any { x ->
        bounds.any { y -> field[x][y] != newField[x][y] }
    }

    override fun toString() = buildString {
        for (y in bounds) {
            bounds.forEach { append(tiles[field[it][y]]) }
            appendLine()
        }
    }

    companion object {
        @JvmStatic
        val games = mutableMapOf<Snowflake, Game2048>()
        @JvmStatic
        val tiles = mapOf(
            0 to "⬛",
            2 to "<:2_:782603657535291403>",
            4 to "<:4_:782603657505144842>",
            8 to "<:8_:782603657471328306>",
            16 to "<:16:782603657525854228>",
            32 to "<:32:782603657682092042>",
            64 to "<:64:782603657665314846>",
            128 to "<:128:782603657686286376>",
            256 to "<:256:782603657774235668>",
            512 to "<:512:782603657660858398>",
            1024 to "<:1024:782603657896132638>",
            2048 to "<:2048:782603657820110871>",
            4096 to "<:4096:782603657719840810>",
            8192 to "<:8192:782603657681436692>",
            16384 to "<:16384:782603657673572392>",
        )
    }
}

suspend fun ButtonInteractionCreateEvent.handle2048buttons() {
    println("2048")
    val game = Game2048.games[interaction.user.id] ?: return
    when (interaction.componentId) {
        "2048left" -> game.moveHorizontally(-1)
        "2048up" -> game.moveVertically(-1)
        "2048right" -> game.moveHorizontally(1)
        "2048down" -> game.moveVertically(1)
    }
    var isGameOver = game.isGameOver
    if (interaction.componentId == "2048stop") isGameOver = true
    if (isGameOver) {
        getUser(interaction.user.id.value).apply { credit += game.score }
        Game2048.games.remove(interaction.user.id)
        println("${interaction.user.username} stopped playing")
    }
    interaction.updateEphemeralMessage {
        content = game.toString()
        embed {
            title = if (isGameOver) "Игра окончена! Счёт: **${game.score}**" else "Счёт: ${game.score}"
        }
        if (!isGameOver) buttons()
    }
}

suspend fun GCICICE.play2048(size: Int) {
    val game = Game2048(size)
    println("${interaction.user.username} started playing")
    interaction.respondEphemeral {
        content = game.toString()
        embed {
            title = "Счёт: 0"
        }
        buttons()
    }
    Game2048.games[interaction.user.id] = game
}

private fun MessageCreateBuilder.buttons() {
    actionRow {
        interactionButton(ButtonStyle.Secondary, "n0") {
            label = " "
        }
        interactionButton(ButtonStyle.Secondary, "2048up") {
            emoji = DiscordPartialEmoji(name = "\uD83D\uDD3C")
        }
        interactionButton(ButtonStyle.Secondary, "n1") {
            label = " "
        }
    }
    actionRow {
        interactionButton(ButtonStyle.Secondary, "2048left") {
            emoji = DiscordPartialEmoji(name = "◀️")
        }
        interactionButton(ButtonStyle.Secondary, "n2") {
            label = " "
        }
        interactionButton(ButtonStyle.Secondary, "2048right") {
            emoji = DiscordPartialEmoji(name = "▶️")
        }
    }
    actionRow {
        interactionButton(ButtonStyle.Secondary, "n3") {
            label = " "
        }
        interactionButton(ButtonStyle.Secondary, "2048down") {
            emoji = DiscordPartialEmoji(name = "\uD83D\uDD3D")
        }
        interactionButton(ButtonStyle.Secondary, "2048stop") {
            emoji = DiscordPartialEmoji(name = "\uD83D\uDED1")
        }
    }
}