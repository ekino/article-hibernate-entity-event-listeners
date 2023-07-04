package com.ekino.example.hibernateentityeventlisteners.entity

import org.hibernate.annotations.GenericGenerator
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.function.Function
import javax.persistence.EntityListeners
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.MappedSuperclass

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class AbstractAuditingEntity {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "uuid2")
  var id: String? = null

  @CreatedDate
  var createdDate: Instant? = null

  @LastModifiedDate
  var lastModifiedDate: Instant? = null

  override fun equals(other: Any?) =
    entityEquals(javaClass, this, other, AbstractAuditingEntity::id)

  /**
   * Should base equality on IDs only if not null, otherwise on instance equality.
   *
   * @see [The best way to implement equals, hashCode, and toString with JPA and Hibernate - Database-generated identifiers](https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate)
   */
  private fun <T : Any, I> entityEquals(clazz: Class<T>, thisObject: T, otherObject: Any?, idGetter: Function<T, I>): Boolean {
    if (thisObject === otherObject) {
      return true
    }
    if (!clazz.isInstance(otherObject)) {
      return false
    }
    val leftId: I? = idGetter.apply(thisObject)
    return leftId != null && leftId == idGetter.apply(clazz.cast(otherObject))
  }

  override fun hashCode() = javaClass.hashCode()

  override fun toString() = "${javaClass.simpleName}($id)"

}
