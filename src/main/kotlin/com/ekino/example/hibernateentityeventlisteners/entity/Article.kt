package com.ekino.example.hibernateentityeventlisteners.entity

import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne

@Entity
class Article : AbstractAuditingEntity() {

  var title: String? = null

  var content: String? = null

  @ManyToOne(fetch = FetchType.LAZY)
  var author: Author? = null

  @ManyToMany(fetch = FetchType.LAZY)
  var categories: MutableSet<Category> = mutableSetOf()
}
