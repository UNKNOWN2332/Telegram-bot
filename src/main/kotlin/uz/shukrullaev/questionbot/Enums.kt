package uz.shukrullaev.questionbot


/**
 * @see uz.shukrullaev.questionbot
 * @author Abdulloh
 * @since 18/06/2025 5:20 pm
 */

enum class UserRole {
    USER, ADMIN, OPERATOR
}

enum class Language(val langName: String) {
    UZ("uz"),
    EN("en"),
    RU("ru");

    companion object {
        fun from(value: String): Language? {
            return entries.firstOrNull { it.langName.equals(value, ignoreCase = true) }
        }
    }
}

enum class MessageType {
    QUESTION,
    ANSWER
}

enum class Status {
    CHECK_NOT_SENT,
    CHECK_SENT,
    NOT_ANSWERED,
    ANSWERING,
    ANSWERED
}

enum class SendMessageBundleCode(val code: Int) {
    FIRSTNAME_REG(1)
}

enum class ExceptionsCode(val code: Int) {
    USERNAME_NOTFOUND(100),
    ALREADY_DELETED(102),
    ID_NOT_FOUND(103),
    PHONE_EXISTS(104),
    USERNAME_AND_PHONE_NOTFOUND(105),
    PHONE_NOTFOUND(106),
    USERNAME_OR_PASSWORD_INCORRECT(107),

}

enum class StateCollection {
    START,
    LANGUAGE,
    FULL_NAME,
    PHONE,
    QUESTION,
    ANSWER,
    END,
}