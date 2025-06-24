package uz.shukrullaev.questionbot

import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler


/**
 * @see uz.shukrullaev.questionbot
 * @author Abdulloh
 * @since 18/06/2025 6:01 pm
 */

@ControllerAdvice
class GlobalExceptionHandler(
    private val errorMessageSource: ResourceBundleMessageSource,
) {
    @ExceptionHandler(
        ExceptionUtil::class
    )
    fun handleAppExceptions(ex: ExceptionUtil): ResponseEntity<Any?> {
        return when (ex) {
            is UsernameNotFoundException -> ResponseEntity.badRequest()
                .body(ex.getErrorMessage(errorMessageSource, ex.username))

            is PhoneNotFoundException -> ResponseEntity.badRequest()
                .body(ex.getErrorMessage(errorMessageSource, ex.email))

            is UsernameAndPhoneNotFoundException -> ResponseEntity.badRequest()
                .body(ex.getErrorMessage(errorMessageSource, ex.loginDTO))

            is UserIdNotFoundException -> ResponseEntity.badRequest()
                .body(ex.getErrorMessage(errorMessageSource, ex.id))

            is UsernameOrPasswordIncorrect -> ResponseEntity.badRequest()
                .body(ex.getErrorMessage(errorMessageSource, ex.loginDTO))

            is PhoneOrPasswordIncorrect -> ResponseEntity.badRequest()
                .body(ex.getErrorMessage(errorMessageSource, ex.loginDTO))

        }
    }
}
