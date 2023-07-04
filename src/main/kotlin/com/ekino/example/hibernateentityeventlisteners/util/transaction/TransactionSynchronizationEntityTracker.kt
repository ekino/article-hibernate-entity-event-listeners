package com.ekino.example.hibernateentityeventlisteners.util.transaction

import org.springframework.transaction.support.TransactionSynchronizationManager
import java.io.Serializable
import java.util.Collections

class TransactionSynchronizationEntityTracker {

  private val trackedObjects: MutableList<Any> = Collections.synchronizedList(mutableListOf())

  companion object {
    private const val OBJECT_TRACKER_KEY_PREFIX = "TransactionSynchronizationEntityTracker"

    @JvmStatic
    fun trackerOfCurrentTransactionEntity(id: Serializable, clazz: Class<*>): TransactionSynchronizationEntityTracker =
      "$OBJECT_TRACKER_KEY_PREFIX.${clazz.canonicalName}.$id"
        .let { trackerBindingKey ->
          TransactionSynchronizationManager.getResource(trackerBindingKey) as? TransactionSynchronizationEntityTracker
            ?: TransactionSynchronizationEntityTracker().also {
              TransactionSynchronizationManager.bindResource(
                trackerBindingKey,
                it,
              )
            }
        }
  }

  fun track(objectEntry: Any) {
    trackedObjects.add(objectEntry)
  }

  fun retrieveObjectsAndClear(): List<Any> {
    synchronized(trackedObjects) {
      return trackedObjects.toList().also { trackedObjects.clear() }
    }
  }
}
