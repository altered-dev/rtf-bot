import commands.createCommands
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import model.CatWives
import model.Users
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

internal lateinit var bot: Kord
internal val guildId = Snowflake(885818276687540264)

suspend fun main() {
    val database = Database.connect("jdbc:h2:file:./build/db", "org.h2.Driver")
    transaction(database) { SchemaUtils.create(Users, CatWives) }
    bot = Kord(Config.TOKEN)
    createCommands()
    bot.login {
        presence { watching("за пекусами") }
    }
}
