package com.ekino.example.hibernateentityeventlisteners

import com.ekino.example.hibernateentityeventlisteners.entity.AbstractAuditingEntity
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.atomic.AtomicInteger
import javax.persistence.EntityManager

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureTestDatabase
abstract class AbstractIntegrationTest {

  val logger = KotlinLogging.logger {}

  @Autowired
  lateinit var transactionManager: PlatformTransactionManager

  @Autowired
  lateinit var entityManager: EntityManager

  val transactionCount = AtomicInteger(0)

  fun inTransaction(title: String? = null, block: (status: TransactionStatus) -> Unit) {
    TransactionTemplate(transactionManager).apply {
      propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
      val transactionId = transactionCount.incrementAndGet()
      execute { status ->
        logger.debug {
          TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
              override fun afterCompletion(status: Int) {
                val statusAsText = when (status) {
                  TransactionSynchronization.STATUS_COMMITTED -> "COMMITTED"
                  TransactionSynchronization.STATUS_ROLLED_BACK -> "ROLLED_BACK"
                  TransactionSynchronization.STATUS_UNKNOWN -> "UNKNOWN"
                  else -> "???"
                }
                logger.debug { "<<< Transaction #$transactionId ended with status '$statusAsText'" }
              }
            },
          )
          buildString {
            append(">>> Transaction #$transactionId")
            title?.also { append(" '$it'") }
            append(""" started...""")
          }
        }
        block(status)
      }
    }
  }

  fun <T> EntityId<T>.findOrNull(): T? = entityManager.find(type, id)

  fun <T> EntityId<T>.find(): T = findOrNull() ?: error("Unable to find ${type.simpleName}(${id})")

  fun <T> EntityId<T>.reference(): T = entityManager.getReference(type, id)
}

data class EntityId<T>(
  val id: String,
  val type: Class<T>,
) {
  companion object {
    inline fun <reified T> of(id: String) = EntityId(id = id, type = T::class.java)
  }
}

inline fun <reified T : AbstractAuditingEntity> T.entityId() = EntityId.of<T>(id = requireNotNull(id) { "Missing id" })
