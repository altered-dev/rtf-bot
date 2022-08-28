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
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction as GCICI

suspend fun GCICI.playRoulette() {
    val amount by command.integers
    val bets = (0 until 3).mapNotNull { command.integers["bet_$it"] }

    val result = Random.nextLong(0L..36L)
    val response = if (result in bets) respondEphemeral {
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
    } else respondEphemeral {
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