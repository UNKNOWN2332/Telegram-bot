package uz.shukrullaev.questionbot

import org.springframework.stereotype.Component
import java.util.*


/**
 * @see uz.shukrullaev.questionbot
 * @author Abdulloh
 * @since 20/06/2025 10:45 am
 */

@Component
class MessageSourceUtil(private val userRepository: UserRepository) {
    fun getLocale(userId: Long): Locale {
        val user = userRepository.findByTelegramIdAndDeletedFalse(userId).get()
        val locale = Locale(user.languages.map { it?.langName }.firstOrNull())
        return locale
    }

    fun getLanguage(userId: Long): MutableSet<String?> {
        return userRepository.findByTelegramIdAndDeletedFalse(userId)
            .orElseGet(null)
            .languages
            .map { it!!.langName }
            .toMutableSet()

    }
}