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
    private val queueRepository: QueueRepository,
) {

    @HandleMessage("/start")
    fun start(update: Update) {
        val userId = getUserId(update)

        val userAccount = userRepository.findByTelegramIdAndDeletedFalse(userId)
            .getOrElse { userRepository.save(UserAccount(userId)) }

        if (userDataIsIncomplete(userAccount)) {
            promptUserRegistration(update)
            return
        }

        val profileCompleted = ensureUserProfileCompleted(update, userAccount)

        if (!profileCompleted) {
            val locale = messageSourceUtil.getLocale(userId)
            buttonUtils.sendToOperatorIfRoleNotUser(userAccount, locale)
        }
    }

    @HandleMessage("/finish")
    fun finishChat(update: Update) {
        val operatorId = getUserId(update)
        val locale = messageSourceUtil.getLocale(operatorId)
        var message = messageSource.getMessage("user_not_found", null, locale)
        val state = strategy.findById(operatorId).get()

        var userAccount = userRepository.findByTelegramIdAndDeletedFalse(operatorId)
            .getOrElse {
                fluentTemplate.sendText(message, operatorId)
                return
            }

        message = messageSource.getMessage("user_have_not_permission", null, locale)
        userAccount.takeIf { it.role == UserRole.OPERATOR }
            ?.let {
                if (!userAccount.isBusy!!) {
                    message = messageSource.getMessage("free", null, locale)
                    fluentTemplate.sendText(message,operatorId)
                    return
                }
                var queues = queueRepository.findAllRelationUserByOperatorIdOperator(userAccount.id!!)
                queues.map {
                    it.status = Status.ANSWERED
                }
                val queue = queues.first()
                val user = queue.user
                queues = queueRepository.saveAll(queues)
                userAccount.apply {
                    isBusy = false
                }
                userAccount = userRepository.save(userAccount)
                val stateUser = strategy.findById(user.telegramId).get()
                state.nextState(StateCollection.START)
                stateUser.nextState(StateCollection.START)
                message = messageSource.getMessage("end_chat_for_user", null, locale)
                fluentTemplate.sendText(message, user.telegramId)
                message = messageSource.getMessage("end_chat_for_operator", null, locale)
                fluentTemplate.sendText(message, operatorId)
                return
            } ?: fluentTemplate.sendText(message, operatorId)
    }

    private fun getUserId(update: Update) = UpdateUtils.getUserId(update)

    private fun userDataIsIncomplete(userAccount: UserAccount) =
        userAccount.languages.firstOrNull() == null ||
                (userAccount.languages.first() == Language.UZ && userAccount.fullName == null && userAccount.phone == null)

    private fun promptUserRegistration(update: Update) {
        val button = buttonUtils.chooseLanguage()
        fluentTemplate.sendText("Til tanlang", button)
    }

    private fun ensureUserProfileCompleted(update: Update, userAccount: UserAccount): Boolean {
        val userId = getUserId(update)
        val state = strategy.findById(userId).orElse(null)
        val locale = messageSourceUtil.getLocale(userAccount.telegramId)

        val missingStep = findFirstMissingStep(userAccount)

        missingStep?.let { action ->
            state?.nextState(action.nextState)
            val message = messageSource.getMessage(action.messageKey, null, locale)
            fluentTemplate.sendText(message, userAccount.telegramId)
            return true
        }
        return false
    }

    private fun findFirstMissingStep(userAccount: UserAccount): RegisterAction? {
        return when {
            userAccount.fullName == null -> RegisterAction(StateCollection.FULL_NAME, "full_name")
            userAccount.phone == null -> RegisterAction(StateCollection.PHONE, "phone_number")
            else -> null
        }
    }
}
