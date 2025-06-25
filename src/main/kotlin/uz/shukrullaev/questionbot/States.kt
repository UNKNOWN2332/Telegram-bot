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
private fun isCommandLine(text: String) = text.startsWith("/start")
private fun isOperatorCommandLine(text: String) = text.startsWith("/finish")

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
        val text = update.message.text
        if (isCommandLine(text)) {
            state.nextState(StateCollection.START)
            return
        }
        checkUserOrElseCreate(userId)
            .apply { fullName = text }
            .let(userRepository::save)
            .takeIf { it.phone != null }
            ?.let {
                state.nextState(StateCollection.END)
                fluentTemplate.sendText(
                    messageSource.getMessage("confirm_phone_number", null, locale),
                    userId,
                    buttonUtils.requestContact(locale)
                )
                return
            }

        state.nextState(StateCollection.PHONE)
        fluentTemplate.sendText(
            messageSource.getMessage("phone_number", null, locale),
            userId,
            buttonUtils.requestContact(locale)
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
        val phone = update.message?.contact?.phoneNumber
        val text = update.message?.text
        val locale = messageSourceUtil.getLocale(userId)
        if (text?.let { isCommandLine(it) } == true) {
            state.nextState(StateCollection.START)
            return
        }

        userRepository.findByTelegramIdAndDeletedFalse(userId)
            .getOrElse { userRepository.save(UserAccount(userId)) }
            .apply { this.phone = phone }
            .let(userRepository::save)
            .also {
                buttonUtils.sendToOperatorIfRoleNotUser(it, locale)
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
    private fun extractContent(update: Update): String? {
        val message = update.message

        return when {
            !message.text.isNullOrBlank() -> message.text
            !message.photo.isNullOrEmpty() -> message.photo.last().fileId // Eng yuqori sifatli rasm
            message.animation != null -> message.animation.fileId
            message.video != null -> message.video.fileId
            else -> null
        }
    }

    override fun onReceive(update: Update, state: State) {
        val userId = UpdateUtils.getUserId(update)
        val content = extractContent(update)

        if (content.isNullOrBlank()) {
            fluentTemplate.sendText("Iltimos, matn, rasm yoki gif yuboring 😊", userId)
            return
        }
        if (content.javaClass == String::class.java && isCommandLine (content)) {
            state.nextState(StateCollection.START)
            return
        }

        val locale = messageSourceUtil.getLocale(userId)
        val language = messageSourceUtil.getLanguage(userId)
        val messageId = update.message.messageId
        val replyMessageId = update.message?.replyToMessage?.messageId

        val userAccount = userRepository.findByTelegramIdAndDeletedFalse(userId)
            .getOrElse { userRepository.save(UserAccount(userId)) }

        saveQueue(content, messageId, replyMessageId, userAccount)

        val operators = getOperators(userAccount.id!!, language)
        if (operators.isEmpty()) {
            fluentTemplate.sendText(messageSource.getMessage("operator_is_busy", null, locale), userId)
            return
        }

        val operator = operators.first().apply { isBusy = true }
        userRepository.save(operator)

        val pendingQueues = queueRepository.findAllByUserIdAndOperatorIsNullAndStatusIsNotAnswered(userAccount.id!!)
            .onEach { it.operator = operator; it.status = Status.CHECK_NOT_SENT }
            .also { queueRepository.saveAll(it) }

        strategy.findById(operator.telegramId).get().nextState(StateCollection.ANSWER)

        if (replyMessageId != null) {
            fluentTemplate.sendText(content, operator.telegramId, replyMessageId - 1)
            return
        }

        pendingQueues
            .filter { it.status == Status.NOT_ANSWERED || it.status == Status.CHECK_NOT_SENT }
            .onEach {
                fluentTemplate.sendText(it.message?.message, operator.telegramId)
                it.status = Status.CHECK_SENT
            }
            .also { queueRepository.saveAll(it) }
    }

    private fun saveQueue(
        question: String,
        messageId: Int,
        replyMessageId: Int?,
        userAccount: UserAccount,
    ) = queueRepository.save(
        Queue(
            Status.NOT_ANSWERED, messageId, replyMessageId,
            messageRepository.save(Message(question, MessageType.QUESTION)),
            userAccount
        )
    )

    private fun getOperators(userId: Long, language: MutableSet<String?>) =
        userRepository.findOperator(userId, language)


    override fun state() = StateCollection.QUESTION
}

@Component
class AnswerState(
    private val fluentTemplate: FluentTemplate,
    private val userRepository: UserRepository,
    private val messageSource: MessageSource,
    private val messageSourceUtil: MessageSourceUtil,
    private val buttonUtils: ButtonUtils,
    private val queueRepository: QueueRepository,
    private val strategy: StateRepositoryStrategy,
    private val messageRepository: MessageRepository,
    private val generalTelegramApi: GeneralTelegramApi,
) : StateAction<StateCollection> {

    override fun onReceive(update: Update, state: State) {

        val userId = UpdateUtils.getUserId(update)
        val text = update.message.text
        if (isCommandLine(text)) {
            state.nextState(StateCollection.START)
            generalTelegramApi.start(update)
            return
        } else if (isOperatorCommandLine(text)) {
            generalTelegramApi.finishChat(update)
            return
        }
        val locale = messageSourceUtil.getLocale(userId)
        val messageId = update.message.messageId
        val replyMessageId = update.message?.replyToMessage?.messageId

        val operatorAccount = userRepository.findByTelegramIdAndDeletedFalse(userId)
            .getOrElse { userRepository.save(UserAccount(userId)) }
        val queues = queueRepository.findAllRelationUserByOperatorId(operatorAccount.id!!)
        val userAccount = queues.firstOrNull()?.user
        queues.map {
            it.status = Status.ANSWERING
        }
        val queue = userAccount?.let { saveQueue(text, messageId, replyMessageId, it, operatorAccount) }
        val message = messageSource.getMessage("operator_name", null, locale)
        if (replyMessageId != null) {
            fluentTemplate.sendText(text, queue?.user!!.telegramId, replyMessageId - 1)
            return
        }
        fluentTemplate.sendText(text, queue?.user!!.telegramId)
    }

    private fun saveQueue(
        question: String,
        messageId: Int,
        replyMessageId: Int?,
        userAccount: UserAccount,
        operatorAccount: UserAccount,
    ) = queueRepository.save(
        Queue(
            Status.ANSWERING, messageId, replyMessageId,
            messageRepository.save(Message(question, MessageType.ANSWER)),
            userAccount, operatorAccount
        )
    )

    override fun state() = StateCollection.ANSWER
}
