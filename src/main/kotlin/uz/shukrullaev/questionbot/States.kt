package uz.shukrullaev.questionbot

import org.khasanof.service.template.FluentTemplate
import org.khasanof.state.State
import org.khasanof.state.StateAction
import org.khasanof.state.repository.StateRepositoryStrategy
import org.khasanof.utils.UpdateUtils
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import kotlin.jvm.optionals.getOrElse


/**
 * @see uz.shukrullaev.questionbot
 * @author Abdulloh
 * @since 19/06/2025 4:24 pm
 */

@Component
class FullNameState(
    private val fluentTemplate: FluentTemplate,
    private val userRepository: UserRepository,
    private val messageSource: MessageSource,
    private val messageSourceUtil: MessageSourceUtil,
    private val buttonUtils: ButtonUtils,
) : StateAction<StateCollection> {

    override fun onReceive(update: Update, state: State) {
        val userId = UpdateUtils.getUserId(update)
        val locale = messageSourceUtil.getLocale(userId)

        checkUserOrElseCreate(userId)
            .apply { fullName = update.message.text }
            .let(userRepository::save)
            .takeIf { it.phone != null }
            ?.let {
                state.nextState(StateCollection.END)
                fluentTemplate.sendText(
                    messageSource.getMessage("confirm_phone_number", null, locale),
                    userId,
                    buttonUtils.confirmPhoneNumberButtons(locale)
                )
                return
            }

        state.nextState(StateCollection.PHONE)
        fluentTemplate.sendText(
            messageSource.getMessage("phone_number", null, locale),
            userId
        )
    }


    override fun state(): StateCollection {
        return StateCollection.FULL_NAME
    }

    private fun checkUserOrElseCreate(userId: Long): UserAccount {
        val userAccount = userRepository.findByTelegramIdAndDeletedFalse(userId).getOrElse {
            userRepository.save(UserAccount(userId))
        }
        return userAccount
    }
}

@Component
class PhoneState(
    private val fluentTemplate: FluentTemplate,
    private val userRepository: UserRepository,
    private val messageSource: MessageSource,
    private val messageSourceUtil: MessageSourceUtil,
    private val buttonUtils: ButtonUtils,
) : StateAction<StateCollection> {

    override fun onReceive(update: Update, state: State) {
        val userId = UpdateUtils.getUserId(update)
        val phone = update.message.text
        val locale = messageSourceUtil.getLocale(userId)

        userRepository.findByTelegramIdAndDeletedFalse(userId)
            .getOrElse { userRepository.save(UserAccount(userId)) }
            .apply { this.phone = phone }
            .let(userRepository::save)
            .also {
                val message = messageSource.getMessage("menu", null, locale)
                val menu = buttonUtils.menu(locale)
                fluentTemplate.sendText(message, userId, menu)
                state.nextState(StateCollection.END)
            }
    }


    override fun state(): StateCollection {
        return StateCollection.PHONE
    }
}

@Component
class QuestionState(
    private val fluentTemplate: FluentTemplate,
    private val userRepository: UserRepository,
    private val messageSource: MessageSource,
    private val messageSourceUtil: MessageSourceUtil,
    private val buttonUtils: ButtonUtils,
    private val queueRepository: QueueRepository,
    private val strategy: StateRepositoryStrategy,
    private val messageRepository: MessageRepository,
) : StateAction<StateCollection> {

    override fun onReceive(update: Update, state: State) {
        val userId = UpdateUtils.getUserId(update)
        val question = update.message.text
        val locale = messageSourceUtil.getLocale(userId)
        val language = messageSourceUtil.getLanguage(userId)
        val messageId = update.message.messageId
        val replyMessageId = update.message?.replyToMessage?.messageId

        val userAccount = userRepository.findByTelegramIdAndDeletedFalse(userId)
            .getOrElse { userRepository.save(UserAccount(userId)) }
        val queue = saveQueue(question, messageId, replyMessageId, userAccount)


        val operators = getOperators(language)
        if (operators.isEmpty()) {
            val message = messageSource.getMessage("operator_is_busy", null, locale)
            fluentTemplate.sendText(message, userId)
        }
        val operator = operators.first()
        queue.apply {
            this.operator = operator
        }
        saveQueue(queue)
        val operatorState = strategy.findById(operator.telegramId).get()
        operatorState.nextState(StateCollection.ANSWER)
        val messageToUser = messageSource.getMessage("checking", null, locale)
        fluentTemplate.sendText(messageToUser, userId)
        val messageToOperators = messageSource.getMessage("check_question", arrayOf(userAccount.fullName), locale)
        fluentTemplate.sendText(messageToOperators, operator.telegramId)
    }

    private fun saveQueue(
        question: String,
        messageId: Int,
        replyMessageId: Int?,
        userAccount: UserAccount,
    ): Queue {
        val message = Message(question, MessageType.QUESTION)
        messageRepository.save(message)
        val queue = Queue(Status.NOT_ANSWERED, messageId, replyMessageId, message, userAccount)
        return queueRepository.save(queue)
    }

    private fun saveQueue(
        queue: Queue,
    ): Queue {
        return queueRepository.save(queue)
    }

    private fun getOperators(
        language: MutableSet<String?>,
    ): MutableList<UserAccount> {
        return userRepository.findOperator(
            language
        )
    }


    override fun state(): StateCollection {
        return StateCollection.QUESTION
    }
}
