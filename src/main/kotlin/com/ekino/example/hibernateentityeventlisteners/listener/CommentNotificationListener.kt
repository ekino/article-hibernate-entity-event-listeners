package com.ekino.example.hibernateentityeventlisteners.listener

import com.ekino.example.hibernateentityeventlisteners.entity.Comment
import com.ekino.example.hibernateentityeventlisteners.listener.hibernate.EntityCreatedEvent
import com.ekino.example.hibernateentityeventlisteners.listener.hibernate.ifEntityOfType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class CommentNotificationListener {

  @Async
  @EventListener
  fun onEntityCreated(event: EntityCreatedEvent) {
    event.ifEntityOfType<Comment> { comment ->
      logger.info { "Sending email to author ${comment.author?.name}" }
    }
  }
}
