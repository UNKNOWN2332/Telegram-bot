package uz.shukrullaev.questionbot

import org.khasanof.annotation.UpdateController
import org.khasanof.annotation.methods.HandleCallback
import org.khasanof.service.template.FluentTemplate
import org.khasanof.state.repository.StateRepositoryStrategy
import org.khasanof.utils.UpdateUtils.getUserId
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import kotlin.jvm.optionals.getOrElse


/**
 * @see uz.shukrullaev.questionbot
 * @author Abdulloh
 * @since 19/06/2025 2:08 pm
 */

@UpdateController
class LanguageState(
    private val fluentTemplate: FluentTemplate,
    private val messageSource: MessageSource,
    private val userRepository: UserRepository,
    private val strategy: StateRepositoryStrategy,
    private val messageSourceUtil: MessageSourceUtil,
    private val buttonUtils: ButtonUtils,
) {

    @HandleCallback(value = ["uz", "ru", "en"])
    fun setLanguage(update: Update) {
        val userId = getUserId(update)
        val selectedLangCode = update.callbackQuery?.data ?: return
        val selectedLanguage = Language.from(selectedLangCode) ?: Language.UZ

        val state = strategy.findById(userId).orElseThrow()
        state.nextState(StateCollection.LANGUAGE)

        checkUserOrElseCreate(userId)
            .apply { this.languages = mutableSetOf(selectedLanguage) }
            .let(userRepository::save)
            .takeIf { it.fullName != null }
            ?.let {
                val locale = messageSourceUtil.getLocale(userId)
                state.nextState(StateCollection.END)
                fluentTemplate.sendText(
                    messageSource.getMessage("confirm_full_name", null, locale),
                    userId,
                    buttonUtils.confirmFullNameButtons(locale)
                )
                return

            }
        state.nextState(StateCollection.FULL_NAME)
        val locale = messageSourceUtil.getLocale(userId)
        val message = messageSource.getMessage("full_name", null, locale)

        fluentTemplate.sendText(
            message,
            userId
        )
    }

    @HandleCallback(value = ["confirm_phone"])
    fun setPhoneNumber(update: Update) {
        val userId = getUserId(update)
        val state = strategy.findById(userId).orElseThrow()
        state.nextState(StateCollection.PHONE)
        val locale = messageSourceUtil.getLocale(userId)
        val message = messageSource.getMessage("phone_number", null, locale)
        fluentTemplate.sendText(message, userId)
    }

    @HandleCallback(value = ["confirm_name"])
    fun setFullName(update: Update) {
        val userId = getUserId(update)
        val state = strategy.findById(userId).orElseThrow()
        state.nextState(StateCollection.FULL_NAME)
        val locale = messageSourceUtil.getLocale(userId)
        val message = messageSource.getMessage("full_name", null, locale)
        fluentTemplate.sendText(message, userId)
    }

    @HandleCallback(value = ["menu"])
    fun menu(update: Update) {
        val userId = getUserId(update)
        val locale = messageSourceUtil.getLocale(userId)

        val menu = buttonUtils.menu(locale)
        val message = messageSource.getMessage("menu", null, locale)

        fluentTemplate.sendText(message, userId, menu)
    }

    @HandleCallback(value = ["settings"])
    fun settings(update: Update) {
        val userId = getUserId(update)
        val locale = messageSourceUtil.getLocale(userId)
        val settings = buttonUtils.settings(locale)
        val message = messageSource.getMessage("set_data", null, locale)
        fluentTemplate.sendText(message, userId, settings)
    }

    @HandleCallback(value = ["set_lang"])
    fun setLanguageButton(update: Update) {
        val userId = getUserId(update)
        val locale = messageSourceUtil.getLocale(userId)
        val language = buttonUtils.chooseLanguage()
        val message = messageSource.getMessage("language", null, locale)
        fluentTemplate.sendText(message, userId, language)
    }

    @HandleCallback("question")
    fun question(update: Update) {
        val userId = getUserId(update)
        val locale = messageSourceUtil.getLocale(userId)
        val message = messageSource.getMessage("ask_question", null, locale)
        val state = strategy.findById(userId).orElseThrow()
        state.nextState(StateCollection.QUESTION)
        fluentTemplate.sendText(message, userId)
    }

    private fun checkUserOrElseCreate(userId: Long): UserAccount {
        val userAccount = userRepository.findByTelegramIdAndDeletedFalse(userId).getOrElse {
            userRepository.save(UserAccount(userId))
        }
        return userAccount
    }


}