package com.ekino.example.hibernateentityeventlisteners

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@ConfigurationPropertiesScan("com.ekino.example.hibernateentityeventlisteners.properties")
@EnableJpaAuditing
class HibernateEntityEventListenersApplication

fun main(args: Array<String>) {
  runApplication<HibernateEntityEventListenersApplication>(*args)
}
