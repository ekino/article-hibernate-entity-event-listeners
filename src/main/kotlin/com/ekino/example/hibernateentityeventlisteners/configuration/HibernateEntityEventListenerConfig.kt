package com.ekino.example.hibernateentityeventlisteners.configuration

import com.ekino.example.hibernateentityeventlisteners.listener.hibernate.HibernateEntityEventListener
import org.hibernate.event.service.spi.EventListenerRegistry
import org.hibernate.event.spi.EventType
import org.hibernate.internal.SessionFactoryImpl
import org.springframework.context.annotation.Configuration
import javax.annotation.PostConstruct
import javax.persistence.EntityManagerFactory

@Configuration(proxyBeanMethods = false)
class HibernateEntityEventListenerConfig(
  private val entityManagerFactory: EntityManagerFactory,
  private val entityEventListener: HibernateEntityEventListener,
) {

  @PostConstruct
  fun registerListeners() {
    entityManagerFactory.unwrap(SessionFactoryImpl::class.java)
      .serviceRegistry
      .getService(EventListenerRegistry::class.java)
      .apply {
        appendListeners(EventType.POST_COMMIT_INSERT, entityEventListener)
        appendListeners(EventType.POST_COMMIT_UPDATE, entityEventListener)
        appendListeners(EventType.POST_COMMIT_DELETE, entityEventListener)
        appendListeners(EventType.POST_COLLECTION_RECREATE, entityEventListener)
        appendListeners(EventType.POST_COLLECTION_UPDATE, entityEventListener)
        appendListeners(EventType.POST_COLLECTION_REMOVE, entityEventListener)
      }
  }
}
