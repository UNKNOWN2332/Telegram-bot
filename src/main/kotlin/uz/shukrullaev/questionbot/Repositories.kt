package uz.shukrullaev.questionbot

import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.findByIdOrNull
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*


/**
 * @see uz.shukrullaev.questionbot
 * @author Abdulloh
 * @since 18/06/2025 5:36 pm
 */

@NoRepositoryBean
interface BaseRepository<T : BaseEntity> : JpaRepository<T, Long>, JpaSpecificationExecutor<T> {
    fun findByIdAndDeletedFalse(id: Long): T?
    fun existsByIdAndDeletedFalse(id: Long): Boolean?
    fun trash(id: Long): T?
    fun trashList(ids: List<Long>): List<T?>
    fun findAllNotDeleted(): List<T>
    fun findAllNotDeleted(pageable: Pageable): Page<T>
}

class BaseRepositoryImpl<T : BaseEntity>(
    entityInformation: JpaEntityInformation<T, Long>, entityManager: EntityManager,
) : SimpleJpaRepository<T, Long>(entityInformation, entityManager), BaseRepository<T> {

    val isNotDeletedSpecification = Specification<T> { root, _, cb -> cb.equal(root.get<Boolean>("deleted"), false) }

    override fun findByIdAndDeletedFalse(id: Long) = findByIdOrNull(id)?.run { if (deleted) null else this }
    override fun existsByIdAndDeletedFalse(id: Long) = findByIdOrNull(id)?.run { !deleted }

    @Transactional
    override fun trash(id: Long): T? =
        findByIdOrNull(id)?.takeIf { !it.deleted }?.run {
            deleted = true
            save(this)
        }


    override fun findAllNotDeleted(): List<T> = findAll(isNotDeletedSpecification)
    override fun findAllNotDeleted(pageable: Pageable): Page<T> = findAll(isNotDeletedSpecification, pageable)
    override fun trashList(ids: List<Long>): List<T?> = ids.map { trash(it) }
}

@Repository
interface UserRepository : BaseRepository<UserAccount> {
    fun findByPhoneAndDeletedFalse(email: String): Optional<UserAccount>
    fun findByTelegramIdAndDeletedFalse(telegramId: Long): Optional<UserAccount>

    @Query(
        "SELECT DISTINCT u.*, l.languages\n" +
                "               FROM user_account u\n" +
                "                        LEFT JOIN useraccount_languages l ON u.id = l.useraccount_id\n" +
                "                        LEFT JOIN queue q on u.id = q.operator_id\n" +
                "               WHERE l.languages ILIKE ANY (ARRAY [:languages]) AND\n" +
                "                   q.operator_id IS NOT NULL\n" +
                "                   AND q.user_id = :userId\n" +
                "                   AND q.status IN ('CHECK_SENT', 'NOT_ANSWERED', 'CHECK_NOT_SENT')\n" +
                "                   or\n" +
                "                   l.languages ILIKE ANY (ARRAY [:languages])\n" +
                "                 AND u.role = 'OPERATOR'\n" +
                "                 AND u.isbusy = false", nativeQuery = true
    )
    fun findOperator(
        userId: Long,
        @Param("languages") languages: MutableSet<String?>,
    ): MutableList<UserAccount>
}

@Repository
interface MessageRepository : BaseRepository<Message> {}

@Repository
interface QueueRepository : BaseRepository<Queue> {
    @Query(
        value = """
                SELECT q
    FROM Queue q
    LEFT JOIN FETCH q.message
    LEFT JOIN FETCH q.user
    WHERE q.user.id = :userId
      AND q.operator IS NULL
      AND q.status = 'NOT_ANSWERED'
        """
    )
    fun findAllByUserIdAndOperatorIsNullAndStatusIsNotAnswered(userId: Long): MutableList<Queue>

    @Query(
        """
    SELECT q
    FROM Queue q
    LEFT JOIN FETCH q.user ua
    WHERE q.user IS NOT NULL
      AND q.operator.id = :operatorId
      AND q.status IN ('CHECK_SENT', 'NOT_ANSWERED', 'CHECK_NOT_SENT')
"""
    )
    fun findAllRelationUserByOperatorId(operatorId: Long): MutableList<Queue>

    @Query(
        """
    SELECT q
    FROM Queue q
    LEFT JOIN FETCH  q.user ua
    WHERE q.user IS NOT NULL
      AND q.operator.id = :operatorId
      AND q.status IN ('CHECK_SENT', 'NOT_ANSWERED', 'CHECK_NOT_SENT', 'ANSWERING')
"""
    )
    fun findAllRelationUserByOperatorIdOperator(operatorId: Long): MutableList<Queue>

    @Query(
        """
     select count(distinct (q.user_id, q.operator_id))
from queue q
where operator_id = :operatorId
    """, nativeQuery = true
    )
    fun getStatisticOperator(operatorId: Long): Long

}

