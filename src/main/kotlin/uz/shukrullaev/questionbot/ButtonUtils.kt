package uz.shukrullaev.questionbot

import org.khasanof.service.template.FluentTemplate
import org.khasanof.utils.keyboards.inline.InlineKeyboardMarkupBuilder
import org.khasanof.utils.keyboards.reply.ReplyKeyboardMarkupBuilder
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import java.util.*


/**
 * @see uz.shukrullaev.questionbot
 * @author Abdulloh
 * @since 19/06/2025 12:09 pm
 */
@Component
class ButtonUtils(
    val messageSource: MessageSource,
    val fluentTemplate: FluentTemplate,
) {

    fun chooseLanguage(): InlineKeyboardMarkup {
        val builder = InlineKeyboardMarkupBuilder()
        builder.addButton("uz", Language.UZ.langName)
        builder.addButton("ru", Language.RU.langName)
        builder.addButton("en", Language.EN.langName)
        return builder.build()
    }


    fun menu(locale: Locale): InlineKeyboardMarkup {
        val question = messageSource.getMessage("question", null, locale)
        val settings = messageSource.getMessage("settings", null, locale)
        val builder = InlineKeyboardMarkupBuilder()
        builder.addButton(question, "question")
        builder.addRow()
        builder.addButton(settings, "settings")
        return builder.build()
    }

    fun menuOperator(locale: Locale): InlineKeyboardMarkup {
        val question = messageSource.getMessage("is_online", null, locale)
        val settings = messageSource.getMessage("settings_operators", null, locale)
        val statistic = messageSource.getMessage("statistics", null, locale)
        val builder = InlineKeyboardMarkupBuilder()
        builder.addButton(question, "is-online")
        builder.addRow()
        builder.addButton(settings, "settings")
        builder.addRow()
        builder.addButton(statistic,"statistics")
        return builder.build()
    }

    fun settings(locale: Locale): InlineKeyboardMarkup {
        val builder = InlineKeyboardMarkupBuilder()
        val setLang = messageSource.getMessage("set_language", null, locale)
        val setName = messageSource.getMessage("set_name", null, locale)
        val setPhone = messageSource.getMessage("set_phone", null, locale)
        builder.addButton(setLang, "set_lang")
        builder.addRow()
        builder.addButton(setName, "confirm_name")
        builder.addButton(setPhone, "confirm_phone")
        return builder.build()
    }

    fun confirmPhoneNumberButtons(locale: Locale, userAccount: UserAccount): InlineKeyboardMarkup {
        return confirmButtons(locale, userAccount, "confirm_phone", "confirm_phone")
    }

    fun confirmFullNameButtons(locale: Locale, userAccount: UserAccount): InlineKeyboardMarkup {
        return confirmButtons(locale, userAccount, "confirm_name", "confirm_name")
    }


    fun sendToOperatorIfRoleNotUser(it: UserAccount, locale: Locale) {
        val (menuMarkup, menuMessageKey) = if (it.role == UserRole.OPERATOR) {
            menuOperator(locale) to "menu_operator"
        } else {
            menu(locale) to "menu"
        }
        val message = messageSource.getMessage(menuMessageKey, null, locale)
        fluentTemplate.sendText(message, menuMarkup)
    }

    fun confirmButtons(
        locale: Locale,
        userAccount: UserAccount,
        confirmTextKey: String,
        confirmCallback: String,
    ): InlineKeyboardMarkup {
        val builder = InlineKeyboardMarkupBuilder()

        val menuKey = if (userAccount.role == UserRole.OPERATOR) "menu_operator" else "menu"
        val menuCallback = if (userAccount.role == UserRole.OPERATOR) "menu_operator" else "menu"

        val confirmText = messageSource.getMessage(confirmTextKey, null, locale)
        val menuText = messageSource.getMessage(menuKey, null, locale)

        builder.addButton(confirmText, confirmCallback)
        builder.addRow()
        builder.addButton(menuText, menuCallback)

        return builder.build()
    }

    fun requestContact(locale: Locale): ReplyKeyboardMarkup {
        val builder = ReplyKeyboardMarkupBuilder()
        val contactPhone = messageSource.getMessage("contact_phone", null, locale)
        builder.addButton(contactPhone).requestContact(true)
        builder.addRow()
        builder.oneTimeKeyboard(true)
        builder.resizeKeyboard(true)
        return builder.build()
    }

}