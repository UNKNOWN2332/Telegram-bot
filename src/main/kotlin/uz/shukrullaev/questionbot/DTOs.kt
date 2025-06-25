package uz.shukrullaev.questionbot


/**
 * @see uz.shukrullaev.questionbot
 * @author Abdulloh
 * @since 18/06/2025 5:25 pm
 */

import jakarta.validation.constraints.Pattern
import java.time.Instant
import java.util.*


data class BaseMessage(val code: Int, val message: String?)
data class SendMessageBundle(val message: String)

data class UserAccountRequestDto(
    val telegramId: Long,
    @field:Pattern(
        regexp = "^\\+998([- ])?(90|91|93|94|95|98|99|33|97|71)([- ])?(\\d{3})([- ])?(\\d{2})([- ])?(\\d{2})$",
        message = "Telifon Raqam notogri misol +998331234567)"
    )
    val phone: String,
    val fullName: String?,
    val role: UserRole,
    val language: MutableSet<Language?> = mutableSetOf(Language.UZ), // faqat USER uchun
    val isBusy: Boolean?,    // faqat OPERATOR uchun
    val password: String?,    // faqat ADMIN uchun
)

fun UserAccountRequestDto.toEntity(): UserAccount {
    return UserAccount(
        telegramId = telegramId,
        phone = phone,
        fullName = fullName,
        role = role,
        languages = language,
        isBusy = isBusy,
        password = password,
        createdAt = Instant.now()
    )
}

data class UserAccountResponseDto(
    val id: Long?,
    val telegramId: Long?,
    val phone: String?,
    val fullName: String?,
    val role: UserRole?,
    val language: MutableSet<Language?>,
    val isBusy: Boolean?,
    val createdAt: Instant?,
)

fun UserAccount.toDTO(): UserAccountResponseDto {
    return UserAccountResponseDto(
        id = id!!,
        telegramId = telegramId,
        phone = phone,
        fullName = fullName,
        role = role,
        language = languages,
        isBusy = isBusy,
        createdAt = createdAt
    )
}

data class MessageRequestDto(
    val message: String,
    val type: MessageType,
    val filePath: String,
) {
    fun toEntity(dto: MessageRequestDto): Message {
        return Message(
            message = dto.message,
            type = dto.type,
            createdAt = Instant.now(),
            filePath = filePath
        )
    }
}

data class MessageResponseDto(
    val message: String?,
    val type: MessageType,
    val createdAt: Instant,
    val filePath: String?,
) {
    fun toDTO(entity: Message): MessageResponseDto {
        return MessageResponseDto(
            message = entity.message,
            type = entity.type,
            createdAt = entity.createdAt,
            filePath = filePath
        )
    }
}
data class ExtractedContent(
    val type: String, // text, photo, animation, video, sticker, voice, audio
    val value: String // matn yoki fileId
)
data class TokenDTO(
    val token: String?,
    val tokenCreateAt: Date?,
    val expiredTime: Date?,
)

fun TokenDTO.toDTO(): TokenDTO = TokenDTO(
    token = this.token,
    tokenCreateAt = this.tokenCreateAt,
    expiredTime = this.expiredTime
)

data class LoginDTO(
    @field:Pattern(
        regexp = "^\\+998([- ])?(90|91|93|94|95|98|99|33|97|71)([- ])?(\\d{3})([- ])?(\\d{2})([- ])?(\\d{2})$",
        message = "Telifon Raqam notogri misol +998331234567)"
    )
    val phone: String? = null,
    val password: String? = null,
)

data class SetRoleDTO(
    @field:Pattern(
        regexp = "^\\+998([- ])?(90|91|93|94|95|98|99|33|97|71)([- ])?(\\d{3})([- ])?(\\d{2})([- ])?(\\d{2})$",
        message = "Telifon Raqam notogri misol +998331234567)"
    )
    val phone: String? = null,
)

data class RegisterAction(
    val nextState: StateCollection,
    val messageKey: String,
)





