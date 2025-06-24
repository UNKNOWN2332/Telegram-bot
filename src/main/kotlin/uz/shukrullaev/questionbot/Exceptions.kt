package uz.shukrullaev.questionbot

import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource
import java.util.*


/**
 * @see uz.shukrullaev.questionbot
 * @author Abdulloh
 * @since 18/06/2025 5:58 pm
 */

sealed class ExceptionUtil(message: String? = null) : RuntimeException(message) {
    abstract fun exceptionType(): ExceptionsCode
    fun getErrorMessage(errorMessageSource: ResourceBundleMessageSource, vararg array: Any?): BaseMessage {
        return BaseMessage(
            exceptionType().code, errorMessageSource.getMessage(
                exceptionType().toString(), array, Locale(
                    LocaleContextHolder.getLocale().language
                )
            )
        )
    }
}

class UsernameNotFoundException(val username: String?) : ExceptionUtil() {
    override fun exceptionType() = ExceptionsCode.USERNAME_NOTFOUND
}

class PhoneNotFoundException(val email: String?) : ExceptionUtil() {
    override fun exceptionType() = ExceptionsCode.PHONE_NOTFOUND
}

class UsernameAndPhoneNotFoundException(val loginDTO: LoginDTO?) : ExceptionUtil() {
    override fun exceptionType() = ExceptionsCode.USERNAME_AND_PHONE_NOTFOUND
}

class UserIdNotFoundException(val id: Long?) : ExceptionUtil() {
    override fun exceptionType() = ExceptionsCode.ID_NOT_FOUND
}

class UsernameOrPasswordIncorrect(val loginDTO: LoginDTO?) : ExceptionUtil() {
    override fun exceptionType() = ExceptionsCode.USERNAME_OR_PASSWORD_INCORRECT
}

class PhoneOrPasswordIncorrect(val loginDTO: LoginDTO?) : ExceptionUtil() {
    override fun exceptionType() = ExceptionsCode.USERNAME_OR_PASSWORD_INCORRECT
}
