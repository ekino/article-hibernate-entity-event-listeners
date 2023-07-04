package com.ekino.example.hibernateentityeventlisteners.configuration

import com.ekino.example.hibernateentityeventlisteners.properties.AsyncExecutorProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.CustomizableThreadFactory
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

private val logger = KotlinLogging.logger {}

@EnableAsync
@Configuration(proxyBeanMethods = false)
class AsyncExecutorConfiguration(
  private val asyncExecutorProperties: AsyncExecutorProperties,
) : AsyncConfigurer {

  private val threadFactory: ThreadFactory by lazy {
    CustomizableThreadFactory().apply {
      setThreadNamePrefix(asyncExecutorProperties.thread.namePrefix)
    }
  }

  override fun getAsyncExecutor(): Executor =
    asyncExecutorProperties.thread.poolSize
      .let { threadPoolSize ->
        logger.info { "Creating Async executor with a thread pool size of $threadPoolSize" }
        Executors.newFixedThreadPool(threadPoolSize, threadFactory)
      }
}
