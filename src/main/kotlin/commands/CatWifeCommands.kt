package commands

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import model.CatWife
import model.User
import org.jetbrains.exposed.sql.transactions.transaction
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent as BICE
import dev.kord.core.event.interaction.GuildApplicationCommandInteractionCreateEvent as GACICE
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent as MSICE

val namePlaceholders = listOf(
    "Джотаро",
    "Неко-чан",
    "Гасаи Юно",
    "Поросль во снегу",
    "Яндекс.Алиса",
)

suspend fun GACICE.giveCatWife() {
    val rarity = CatWife.Rarity.getRandom()
    val imageUrl = ""
    val catWife = transaction {
        CatWife.new {
            User.findById(interaction.user.id.value)?.let { ownerId = it.id }
            this.rarity = rarity
            this.imageUrl = imageUrl
        }
    }
    interaction.respondEphemeral {
        embed {
            title = "Партия гордится тобой!"
            description = "Ты получить новый кошка жена!"
            image = imageUrl
            color = rarity.color
        }
        actionRow {
            interactionButton(ButtonStyle.Success, "name_cat_wife_${catWife.id}") {
                label = "Ура! Хочу её назвать"
                emoji = DiscordPartialEmoji(name = "\uD83E\uDD73")
            }
        }
    }
}

suspend fun BICE.nameCatWife() {
    interaction.modal("Новая кошка жена", interaction.componentId) {
        actionRow {
            textInput(TextInputStyle.Short, "name", "Как зовут твою новую кошку жену?") {
                required = true
                this.allowedLength = 1..32
                this.placeholder = namePlaceholders.random()
            }
        }
    }
}

suspend fun MSICE.saveCatWife() {
    val id = interaction.modalId.substringAfterLast('_').toLong()
    val name = interaction.actionRows.first().textInputs["name"]!!.value!!
    val catWife = CatWife.findById(id)?.also { it.name = name } ?: return
    interaction.respondPublic {
        embed {
            title = "У ${interaction.user.mention} появилась новая кошка жена!"
            description = "Её зовут $name!"
            image = catWife.imageUrl
            color = catWife.rarity.color
        }
    }
}