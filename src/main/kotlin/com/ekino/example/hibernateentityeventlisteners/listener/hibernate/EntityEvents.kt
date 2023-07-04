package com.ekino.example.hibernateentityeventlisteners.listener.hibernate

import java.io.Serializable
import kotlin.reflect.KProperty

sealed interface EntityEvent {
  val entityId: Serializable
  val entity: Any
}

inline fun <reified T> EntityEvent.entityOfTypeOrNull() = this.entity as? T

inline fun <reified T> EntityEvent.ifEntityOfType(block: (entity: T) -> Unit) {
  entityOfTypeOrNull<T>()?.also(block)
}

class EntityCreatedEvent(
  override val entityId: Serializable,
  override val entity: Any,
) : EntityEvent

class EntityUpdatedEvent(
  override val entityId: Serializable,
  override val entity: Any,
  val properties: List<PropertyStateDiff>,
) : EntityEvent

class EntityDeletedEvent(
  override val entityId: Serializable,
  override val entity: Any,
) : EntityEvent

interface PropertyStateValue {
  val value: Any?
  val loaded: Boolean

  fun loadedValueOr(defaultValueProvider: () -> Any?) = when {
    loaded -> value
    else -> defaultValueProvider()
  }

  fun loadedValueOrNull() = loadedValueOr { null }
}

internal data class BasicPropertyStateValue(
  override val value: Any?,
  override val loaded: Boolean,
) : PropertyStateValue

interface PropertyState {
  val name: String
  val state: PropertyStateValue
}

enum class StateDiffStatus {
  EQUAL,
  CHANGED,
  UNKNOWN
}

interface PropertyStateDiff : PropertyState {
  val oldState: PropertyStateValue
  val diffStatus: StateDiffStatus
}

internal data class BasicPropertyDiffState(
  override val name: String,
  override val oldState: PropertyStateValue,
  override val state: PropertyStateValue,
  override val diffStatus: StateDiffStatus,
) : PropertyStateDiff

operator fun Collection<PropertyStateDiff>.get(name: String) = firstOrNull { it.name == name }
operator fun Collection<PropertyStateDiff>.get(field: KProperty<*>) = get(field.name)
