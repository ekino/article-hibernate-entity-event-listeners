package com.ekino.example.hibernateentityeventlisteners.listener.hibernate

import com.ekino.example.hibernateentityeventlisteners.util.transaction.TransactionSynchronizationEntityTracker.Companion.trackerOfCurrentTransactionEntity
import io.github.oshai.kotlinlogging.KotlinLogging
import org.hibernate.event.spi.AbstractCollectionEvent
import org.hibernate.event.spi.PostCollectionRecreateEvent
import org.hibernate.event.spi.PostCollectionRecreateEventListener
import org.hibernate.event.spi.PostCollectionRemoveEvent
import org.hibernate.event.spi.PostCollectionRemoveEventListener
import org.hibernate.event.spi.PostCollectionUpdateEvent
import org.hibernate.event.spi.PostCollectionUpdateEventListener
import org.hibernate.event.spi.PostCommitDeleteEventListener
import org.hibernate.event.spi.PostCommitInsertEventListener
import org.hibernate.event.spi.PostCommitUpdateEventListener
import org.hibernate.event.spi.PostDeleteEvent
import org.hibernate.event.spi.PostInsertEvent
import org.hibernate.event.spi.PostUpdateEvent
import org.hibernate.persister.entity.EntityPersister
import org.hibernate.tuple.NonIdentifierAttribute
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.io.Serializable
import javax.persistence.EntityManager

@Component
class HibernateEntityEventListener(
  private val entityManager: EntityManager,
  private val applicationEventPublisher: ApplicationEventPublisher,
) :
  PostCommitInsertEventListener,
  PostCommitUpdateEventListener,
  PostCommitDeleteEventListener,
  PostCollectionUpdateEventListener,
  PostCollectionRecreateEventListener,
  PostCollectionRemoveEventListener {

  companion object {
    private val logger = KotlinLogging.logger {}
  }

  /**
   * <q>It seems, when the PostInsertEventListener is registered as EventType.POST_INSERT, the method is not called at all and the listener is always executed before committing the transaction.
   *
   * However, if the listener is registered as EventType.POST_COMMIT_INSERT the method is actually called and if it returns true the listener will be called after committing the transaction. If it returns false the listener will not be called at all.
   *
   * Also, if the listener is of type PostInsertEventListener the onPostInsert method will be called regardless of whether the transaction was successful or not. If the listener is of type PostCommitInsertEventListener the onPostInsert method will only be called for successful transactions. Otherwise, the onPostInsertCommitFailed method is called.</q>
   *
   * @see <a href="https://stackoverflow.com/a/68192012">What does Hibernate PostInsertEventListener.requiresPostCommitHanding do?</a>
   */
  @Suppress("OVERRIDE_DEPRECATION")
  override fun requiresPostCommitHanding(persister: EntityPersister): Boolean = true

  override fun onPostInsert(event: PostInsertEvent) {
    logger.trace { "Hibernate ${event.typeName()} for ${entityIdentifier(event.id, event.entity)}" }

    EntityCreatedEvent(
      entityId = event.id,
      entity = event.entity,
    )
      .also {
        logger.debug { "Publishing ${it.typeName()} for ${it.entityIdentifier()}" }
      }
      .also(applicationEventPublisher::publishEvent)
  }

  override fun onPostInsertCommitFailed(event: PostInsertEvent) {
    logger.error { "Hibernate ${event.typeName()} failure for ${entityIdentifier(event.id, event.entity)}" }
  }

  override fun onPostUpdateCommitFailed(event: PostUpdateEvent) {
    logger.error { "Hibernate ${event.typeName()} failure for ${entityIdentifier(event.id, event.entity)}" }
  }

  override fun onPostDelete(event: PostDeleteEvent) {
    logger.trace { "Hibernate ${event.typeName()} for ${entityIdentifier(event.id, event.entity)}" }

    EntityDeletedEvent(
      entityId = event.id,
      entity = event.entity,
    )
      .also {
        logger.debug { "Publishing ${it.typeName()} for ${it.entityIdentifier()}" }
      }
      .also(applicationEventPublisher::publishEvent)
  }

  override fun onPostDeleteCommitFailed(event: PostDeleteEvent) {
    logger.error { "Hibernate ${event.typeName()} failure for ${entityIdentifier(event.id, event.entity)}" }
  }

  override fun onPostRecreateCollection(event: PostCollectionRecreateEvent) {
    logger.trace { "Hibernate ${event.typeName()} for ${event.entityIdentifier()} on property '${event.affectedPropertyOrNull().orEmpty()}'" }
  }

  override fun onPostRemoveCollection(event: PostCollectionRemoveEvent) {
    logger.trace { "Hibernate ${event.typeName()} for ${event.entityIdentifier()} on property '${event.affectedPropertyOrNull().orEmpty()}'" }
  }

  override fun onPostUpdate(event: PostUpdateEvent) {
    logger.trace { "Hibernate ${event.typeName()} for ${entityIdentifier(event.id, event.entity)}" }

    trackAndMergeUpdateEvents(event.id, event.entity::class.java, event)
  }

  override fun onPostUpdateCollection(event: PostCollectionUpdateEvent) {
    logger.trace { "Hibernate ${event.typeName()} for ${event.entityIdentifier()} on property '${event.affectedPropertyOrNull().orEmpty()}'" }

    trackAndMergeUpdateEvents(event.affectedOwnerIdOrNull, event.affectedOwnerOrNull::class.java, event)
  }

  private fun trackAndMergeUpdateEvents(entityId: Serializable, entityClass: Class<*>, event: Any) {
    trackerOfCurrentTransactionEntity(entityId, entityClass).apply {
      track(event)
      TransactionSynchronizationManager.registerSynchronization(
        object : TransactionSynchronization {
          override fun afterCompletion(status: Int) {
            retrieveObjectsAndClear()
              .takeIf { it.isNotEmpty() }
              ?.also {
                try {
                  mergeUpdateEvents(it)
                } catch (e: Exception) {
                  logger.error(e) { "Error trying to merge hibernate update events of ${entityClass.name}(${entityId})" }
                  throw e
                }
              }
          }
        },
      )
    }
  }

  private fun mergeUpdateEvents(hibernateEvents: List<Any>) {
    val postCollectionEvents = hibernateEvents.filterIsInstance<AbstractCollectionEvent>()
    val postUpdateEvent = hibernateEvents.filterIsInstance<PostUpdateEvent>().takeIf { it.isNotEmpty() }?.single()

    val (entityId, entity) = findEntityIdWithEntityOrNull(postUpdateEvent, postCollectionEvents) ?: return

    val updatedCollectionStateByPropertyName = extractDiffStateByPropertyName(postCollectionEvents)

    val allPropertyStates = updatedCollectionStateByPropertyName.values +
      postUpdateEvent?.extractDiffStatesOfProperties(updatedCollectionStateByPropertyName.keys).orEmpty()

    EntityUpdatedEvent(
      entityId = entityId,
      entity = entity,
      properties = allPropertyStates,
    )
      .also { logger.debug { eventWithDetailedProperties(it) } }
      .also(applicationEventPublisher::publishEvent)
  }

  private fun findEntityIdWithEntityOrNull(updatedEvent: PostUpdateEvent?, collectionEvents: Collection<AbstractCollectionEvent>): Pair<Serializable, Any>? {
    if (updatedEvent != null) {
      return updatedEvent.id to updatedEvent.entity
    }

    collectionEvents.randomOrNull()?.apply {
      return affectedOwnerIdOrNull to affectedOwnerOrNull
    }

    return null
  }

  private fun extractDiffStateByPropertyName(postCollectionEvents: List<AbstractCollectionEvent>) = postCollectionEvents.asSequence()
    .mapNotNull { it.extractPropertyStateOrNull() }
    .groupBy { it.name }
    .mapValues { (_, states) -> states.sortedWith(loadedFirstComparator()).first() }

  private fun AbstractCollectionEvent.extractPropertyStateOrNull(): BasicPropertyDiffState? {
    val propertyName = affectedPropertyOrNull() ?: return null

    return BasicPropertyDiffState(
      name = propertyName,
      oldState = BasicPropertyStateValue(
        value = null,
        loaded = false,
      ),
      state = BasicPropertyStateValue(
        value = collection,
        loaded = true,
      ),
      diffStatus = StateDiffStatus.CHANGED,
    )
  }

  private fun PostUpdateEvent.extractDiffStatesOfProperties(propertyNamesToIgnore: Collection<String>) =
    persister
      .entityMetamodel
      .properties
      .asSequence()
      .withIndex()
      .filterNot { (_, property) -> propertyNamesToIgnore.contains(property.name) }
      .map { (index, property) -> extractPropertyDiffState(property, index) }
      .toList()

  private fun PostUpdateEvent.extractPropertyDiffState(
    property: NonIdentifierAttribute,
    indexInEvent: Int,
  ): BasicPropertyDiffState {
    val (oldStateValue, newStateValue) = listOf(
      oldState,
      state,
    )
      .map { it[indexInEvent] }
      .map { BasicPropertyStateValue(value = it, loaded = it.isLoadedEntity()) }

    return BasicPropertyDiffState(
      name = property.name,
      oldState = oldStateValue,
      state = newStateValue,
      diffStatus = run status@{
        if (dirtyProperties.contains(indexInEvent)) {
          return@status StateDiffStatus.CHANGED
        }

        if (oldStateValue.loaded && newStateValue.loaded) {
          return@status if (property.type.isEqual(
              oldStateValue.value,
              newStateValue.value,
            )
          ) StateDiffStatus.EQUAL else StateDiffStatus.CHANGED
        }

        StateDiffStatus.UNKNOWN
      },
    )
  }

  private fun Any?.isLoadedEntity() = entityManager.entityManagerFactory.persistenceUnitUtil.isLoaded(this)

  private fun loadedFirstComparator() = compareBy<BasicPropertyDiffState> { if (it.state.loaded) 1 else 2 }

  private fun eventWithDetailedProperties(event: EntityUpdatedEvent) = buildString {
    appendLine("Publishing ${event.typeName()} for ${event.entityIdentifier()} with properties:")
    event.properties.sortedBy { it.name }
      .forEach { property ->
        appendLine("- ${property.name} (${property.diffStatus.name}):")
        when (property.diffStatus) {
          StateDiffStatus.EQUAL -> appendLine("value: \"${property.state.loadedValueOrLazyDefault()}\"".prependIndent())
          StateDiffStatus.CHANGED,
          StateDiffStatus.UNKNOWN,
          -> {
            appendLine("old: \"${property.oldState.loadedValueOrLazyDefault()}\"".prependIndent())
            appendLine("new: \"${property.state.loadedValueOrLazyDefault()}\"".prependIndent())
          }
        }
      }
  }

  private fun PropertyStateValue.loadedValueOrLazyDefault() = loadedValueOr { "<not loaded>" }

  private fun AbstractCollectionEvent.affectedPropertyOrNull() =
    session.persistenceContext.getCollectionEntryOrNull(collection)
      ?.role
      ?.removePrefix(affectedOwnerEntityName)
      ?.removePrefix(".")

  private fun AbstractCollectionEvent.entityIdentifier() = entityIdentifier(affectedOwnerIdOrNull, affectedOwnerOrNull)

  private fun EntityEvent.entityIdentifier() = entityIdentifier(entityId, entity)

  private fun entityIdentifier(id: Serializable, entity: Any) = "${entity.typeName()}($id)"

  private fun Any.typeName() = javaClass.simpleName
}
