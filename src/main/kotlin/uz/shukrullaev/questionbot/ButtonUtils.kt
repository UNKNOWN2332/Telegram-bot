package uz.shukrullaev.questionbot

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

    fun settings(locale: Locale): InlineKeyboardMarkup {
        val builder = InlineKeyboardMarkupBuilder()
        val setLang = messageSource.getMessage("set_language", null, locale)
        val setName = messageSource.getMessage("set_name", null, locale)
        val setPhone = messageSource.getMessage("set_phone", null, locale)
        builder.addButton(setLang,"set_lang")
        builder.addRow()
        builder.addButton(setName,"confirm_name")
        builder.addButton(setPhone,"confirm_phone")
        return builder.build()
    }

    fun confirmPhoneNumberButtons(locale: Locale): InlineKeyboardMarkup {
        val builder = InlineKeyboardMarkupBuilder()
        val confirmPhoneText = messageSource.getMessage("confirm_phone", null, locale)
        val changePhoneText = messageSource.getMessage("menu", null, locale)

        builder.addButton(confirmPhoneText, "confirm_phone")
        builder.addRow()
        builder.addButton(changePhoneText, "menu")

        return builder.build()
    }

    fun confirmFullNameButtons(locale: Locale): InlineKeyboardMarkup {
        val builder = InlineKeyboardMarkupBuilder()
        val confirmPhoneText = messageSource.getMessage("confirm_name", null, locale)
        val changePhoneText = messageSource.getMessage("menu", null, locale)

        builder.addButton(confirmPhoneText, "confirm_name")
        builder.addRow()
        builder.addButton(changePhoneText, "menu")

        return builder.build()
    }

}