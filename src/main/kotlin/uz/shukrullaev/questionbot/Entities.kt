package uz.shukrullaev.questionbot

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.*

/**
 * @see uz.shukrullaev.questionbot
 * @author Abdulloh
 * @since 18/06/2025 5:17 pm
 */

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,

    @CreatedDate
    @Column(updatable = false)
    var createdDate: Instant? = null,

    @LastModifiedDate
    var modifiedDate: Instant? = null,

    @Column(nullable = false)
    var deleted: Boolean = false,
)

@Entity
@Table(name = "user_account")
class UserAccount(
    @Column(nullable = false, unique = true)
    val telegramId: Long,

    @Column(unique = true)
    var phone: String? = null,

    var fullName: String? = null,

    @Enumerated(EnumType.STRING)
    var role: UserRole = UserRole.USER,

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    var languages: MutableSet<Language?> = mutableSetOf(Language.UZ), // faqat USER uchun meaningful

    var isBusy: Boolean? = false, // faqat OPERATOR uchun meaningful

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    val password: String? = null, // faqat ADMIN uchun meaningful


) : BaseEntity()


@Entity
@Table(name = "message")
class Message(
    @Column(columnDefinition = "TEXT")
    val message: String? = null, // Matn bo‘lsa to‘ldiriladi

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: MessageType,

    @Column(nullable = true)
    val filePath: String? = null, // Media bo‘lsa file path saqlanadi

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
) : BaseEntity()

@Entity
@Table(name = "queue")
class Queue(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: Status,

    @Column(nullable = false)
    val telegramMessageId: Int,

    val replaceMessageId: Int? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    var message: Message? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: UserAccount,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id")
    var operator: UserAccount? = null,

    @Column(nullable = false)
    val createdAt: Instant? = Instant.now(),
) : BaseEntity()

@Entity
@Table(name = "user_state")
data class UserState(
    @Id
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var state: StateCollection,
)

