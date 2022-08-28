package games

import commands.NEGATIVE
import commands.POSITIVE
import commands.giveCatWife
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.rest.builder.message.create.embed
import getUser
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.random.Random
import kotlin.random.nextLong
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent as GCICICE

suspend fun GCICICE.playRoulette() {
    val cmd = interaction.command
    val amount by cmd.integers
    val bets = (0 until 3).mapNotNull { cmd.integers["bet_$it"] }
    val user = interaction.user

    val result = Random.nextLong(0L..36L)
    val response = if (result in bets) interaction.respondEphemeral {
        transaction { getUser(user.id.value).credit += amount }
        embed {
            title = "Поздравляем! Вы выиграть рулетка!"
            description = """
                Вы поставить на ${bets.joinToString()}, выпасть на $result
                Мы дать вам **$amount** социальный кредит за ваши заслуги!
            """.trimIndent()
            if (result == 0L) description += "\nА также мы дать вам кошка жена за поставить на зеро!"
            thumbnail { url = POSITIVE }
        }
    } else interaction.respondEphemeral {
        transaction { getUser(user.id.value).credit -= amount }
        embed {
            title = "Как жаль..."
            description = """
                Вы поставить на ${bets.joinToString()}, выпасть на $result
                Вы огорчить партия, поэтому мы забрать **$amount** социальный кредит.
            """.trimIndent()
            thumbnail { url = NEGATIVE }
        }
    }
    if (result in bets && result == 0L) giveCatWife(response)
}