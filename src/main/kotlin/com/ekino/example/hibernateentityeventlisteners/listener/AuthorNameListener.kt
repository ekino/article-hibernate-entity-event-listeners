package com.ekino.example.hibernateentityeventlisteners.listener

import com.ekino.example.hibernateentityeventlisteners.entity.Author
import com.ekino.example.hibernateentityeventlisteners.listener.hibernate.EntityCreatedEvent
import com.ekino.example.hibernateentityeventlisteners.listener.hibernate.EntityUpdatedEvent
import com.ekino.example.hibernateentityeventlisteners.listener.hibernate.StateDiffStatus
import com.ekino.example.hibernateentityeventlisteners.listener.hibernate.get
import com.ekino.example.hibernateentityeventlisteners.listener.hibernate.ifEntityOfType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class AuthorNameListener {

  @EventListener
  fun onEntityCreated(event: EntityCreatedEvent) {
    event.ifEntityOfType<Author> { author ->
      logger.info { "New author named ${author.name}" }
    }
  }

  @EventListener
  fun onEntityUpdated(event: EntityUpdatedEvent) {
    event.ifEntityOfType<Author> {
      event.properties[Author::name]
        ?.takeIf { it.diffStatus == StateDiffStatus.CHANGED }
        ?.also { nameProp ->
          logger.info { """Author previously named "${nameProp.oldState.loadedValueOrNull()}" is now named "${nameProp.state.loadedValueOrNull()}"""" }
        }
    }
  }
}
