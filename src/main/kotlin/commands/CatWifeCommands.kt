package commands

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.core.behavior.interaction.response.createPublicFollowup
import dev.kord.core.behavior.interaction.updatePublicMessage
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import getUser
import model.CatWife
import model.User
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.random.Random
import dev.kord.core.behavior.interaction.response.MessageInteractionResponseBehavior as MIRB
import dev.kord.core.event.interaction.ActionInteractionCreateEvent as AICE
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent as BICE
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent as MSICE

val namePlaceholders = listOf(
    "Джотаро",
    "Неко-чан",
    "Гасаи Юно",
    "Поросль во снегу",
    "Яндекс.Алиса",
)

val catWifeImages = mapOf(
    CatWife.Rarity.COMMON to listOf(
        "https://i.imgur.com/1ksIMXs.png",
        "https://i.imgur.com/opsbhxK.png",
        "https://i.imgur.com/GLpNv4C.png",
    ),
    CatWife.Rarity.UNCOMMON to listOf(
        "https://i.imgur.com/tKYKhNW.png",
        "https://i.imgur.com/nd3yEXj.png",
    ),
    CatWife.Rarity.RARE to listOf(
        "https://i.imgur.com/Xb9fsiD.png",
        "https://i.imgur.com/vqnkL4u.png",
    ),
    CatWife.Rarity.EPIC to listOf(
        "https://i.imgur.com/dYg5qKl.png",
    ),
    CatWife.Rarity.LEGENDARY to listOf(
        "https://i.imgur.com/ZSAMp5D.png",
    ),
    CatWife.Rarity.DEV to listOf(
        "https://i.imgur.com/qUFVYxR.png",
    ),
)

suspend fun AICE.giveCatWife(response: MIRB? = null) {
    val rarity = CatWife.Rarity.getRandom()
    val imageUrl = catWifeImages[rarity]!!.random()
    val catWife = transaction {
        CatWife.new {
            ownerId = getUser(interaction.user.id.value).id
            name = namePlaceholders.random()
            this.rarity = rarity
            this.imageUrl = imageUrl
        }
    }
    if (response == null) interaction.respondEphemeral { catWifeResponse(catWife) }
    else response.createEphemeralFollowup { catWifeResponse(catWife) }
}

private fun MessageCreateBuilder.catWifeResponse(catWife: CatWife) {
    embed {
        title = "\uD83D\uDC3C Партия гордится тобой! \uD83C\uDF5A"
        description = "\uD83D\uDC31 Ты получить новый кошка жена! \uD83D\uDC69"
        image = catWife.imageUrl
        color = catWife.rarity.color
    }
    actionRow {
        interactionButton(
            style = ButtonStyle.Success,
            customId = "namecw_${catWife.id}",
        ) {
            label = "Ура! Хочу её назвать"
            emoji = DiscordPartialEmoji(name = "\uD83E\uDD73")
        }
    }
}

suspend fun BICE.nameCatWife() {
    interaction.modal(
        title = "Новая кошка жена",
        customId = interaction.componentId,
    ) {
        actionRow {
            textInput(
                style = TextInputStyle.Short,
                customId = "name",
                label = "Как зовут твою новую кошку жену?",
            ) {
                required = true
                allowedLength = 1..32
                placeholder = namePlaceholders.random()
            }
        }
    }
}

suspend fun MSICE.saveCatWife() {
    val id = interaction.modalId
        .substringAfterLast('_')
        .toLong()
    val name = interaction.actionRows
        .first()
        .textInputs["name"]!!
        .value!!
    val catWife = transaction { CatWife.findById(id)?.also { it.name = name } } ?: return
    interaction.respondPublic {
        embed {
            title = "\uD83D\uDC3C Партия проявить щедрость за хороший поведение! \uD83C\uDF5A"
            description = "\uD83D\uDC31 Мы выдать ${interaction.user.mention} одна кошка жена - $name! \uD83D\uDC69"
            image = catWife.imageUrl
            color = catWife.rarity.color
        }
        actionRow {
            interactionButton(
                style = ButtonStyle.Danger,
                customId = "stealcw_${catWife.id}"
            ) {
                label = "Украсть кошку жену"
                emoji = DiscordPartialEmoji(name = "\uD83E\uDD0F")
            }
        }
    }
}

suspend fun BICE.stealCatWife() {
    val id = interaction.componentId
        .substringAfterLast('_')
        .toLong()
    val catWife = transaction { CatWife.findById(id) } ?: return
    if (interaction.user.id.value == catWife.ownerId.value)
        interaction.respondEphemeral { content = "Вы не можете украсть свою кошку жену!" }
    else interaction.updatePublicMessage {
        content = "блять"
    }.createPublicFollowup {
        content = if (Random.nextDouble() < 0.3) {
            transaction { catWife.ownerId = getUser(interaction.user.id.value).id }
            "О ужас! Кажется, кошка жена **${catWife.name}** пропала!"
        } else {
            transaction { getUser(interaction.user.id.value).credit -= catWife.cost }
            """
                Позор! ${interaction.user.mention} попытался украсть кошку жену **${catWife.name}**!
                За такой порочный поступок мы забрать у него **${catWife.cost}** социальный кредит!
            """.trimIndent()
        }
    }
}

suspend fun BICE.viewCatWife() {
    val id = interaction.componentId
        .substringAfterLast('_')
        .toLong()
    val catWife = transaction { CatWife.findById(id) } ?: return
    interaction.respondEphemeral {
        embed {
            title = "${catWife.rarity.emoji} ${catWife.name}"
            description = """
                Владелец: <@${catWife.ownerId.value}>
                Редкость: ${catWife.rarity.displayName}
            """.trimIndent()
            color = catWife.rarity.color
            image = catWife.imageUrl
        }
        if (catWife.ownerId.value == interaction.user.id.value) actionRow {
            interactionButton(
                style = ButtonStyle.Danger,
                customId = "sellcw_$id",
            ) {
                label = "Продать за ${catWife.cost} СК"
                emoji = DiscordPartialEmoji(name = "\uD83D\uDCB0")
            }
        }
    }
}

suspend fun BICE.sellCatWife() {
    val id = interaction.componentId.substringAfterLast('_').toLong()
    transaction {
        val catWife = CatWife.findById(id) ?: return@transaction
        val user = User.findById(interaction.user.id.value) ?: return@transaction
        user.credit += catWife.cost
        catWife.delete()
    }
    interaction.respondEphemeral { content = "Кошко-жена успешно продана!" }
}