package com.ekino.example.hibernateentityeventlisteners.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "async.executor")
data class AsyncExecutorProperties(
  val thread: ExecutorThreadProperties,
)

data class ExecutorThreadProperties(
  val namePrefix: String? = null,
  val poolSize: Int,
)
