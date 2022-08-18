import commands.createCommands
import dao.DatabaseFactory
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord

internal lateinit var bot: Kord
internal val guildId = Snowflake(885818276687540264)

suspend fun main() {
    DatabaseFactory.init()
    bot = Kord(Config.TOKEN)
    createCommands()
    bot.login {
        presence { watching("за пекусами") }
    }
}
