package uz.shukrullaev.questionbot

import org.khasanof.annotation.UpdateController
import org.khasanof.annotation.methods.HandleMessage
import org.khasanof.service.template.FluentTemplate
import org.khasanof.state.repository.StateRepositoryStrategy
import org.khasanof.utils.UpdateUtils
import org.springframework.context.MessageSource
import org.telegram.telegrambots.meta.api.objects.Update
import java.util.*
import kotlin.jvm.optionals.getOrElse


/**
 * @see uz.shukrullaev.questionbot
 * @author Abdulloh
 * @since 18/06/2025 5:39 pm
 */

@UpdateController
class GeneralTelegramApi(
    private val fluentTemplate: FluentTemplate,
    private val userRepository: UserRepository,
    private val buttonUtils: ButtonUtils,
    private val messageSource: MessageSource,
    private val strategy: StateRepositoryStrategy,
    private val messageSourceUtil: MessageSourceUtil,

) {
    @HandleMessage("/start")
    fun start(update: Update) {
        val userId = getUserId(update)

        val userAccount = userRepository.findByTelegramIdAndDeletedFalse(userId)
            .getOrElse { userRepository.save(UserAccount(userId)) }
        if (userAccount.languages.firstOrNull() == null || userAccount.languages.firstOrNull() == Language.UZ && userAccount.fullName == null && userAccount.phone == null) {
            userRegister(update)
        } else
            ensureUserProfileCompleted(update, userAccount).runIfFalse {
                val menu = buttonUtils.menu(messageSourceUtil.getLocale(userId))
                val locale = messageSourceUtil.getLocale(userAccount.telegramId)
                val message = messageSource.getMessage("menu", null, locale)

                fluentTemplate.sendText(message, menu)
            }
    }

    private fun getUserId(update: Update): Long = UpdateUtils.getUserId(update)

    fun userRegister(update: Update) {
        val button = buttonUtils.chooseLanguage()
        fluentTemplate.sendText("Til tanlang", button)
    }

    fun ensureUserProfileCompleted(update: Update, userAccount: UserAccount): Boolean {
        val userId = getUserId(update)
        val state = strategy.findById(userId).orElse(null)
        val locale = messageSourceUtil.getLocale(userAccount.telegramId)

        val firstMissingStep = checkMissingStep(userAccount)

        firstMissingStep?.let { action ->
            state?.nextState(action.nextState)
            val message = messageSource.getMessage(action.messageKey, null, locale)
            fluentTemplate.sendText(message, userAccount.telegramId)
            return true
        }
        return false
    }

    private fun checkMissingStep(
        userAccount: UserAccount,
    ): RegisterAction? {
        val firstMissingStep = sequence {
            if (userAccount.fullName == null) {
                yield(RegisterAction(StateCollection.FULL_NAME, "full_name"))
            } else if (userAccount.phone == null) {
                yield(RegisterAction(StateCollection.PHONE, "phone_number"))
            }
        }.firstOrNull()
        return firstMissingStep
    }

}