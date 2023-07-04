package com.ekino.example.hibernateentityeventlisteners.entity

import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.ManyToOne

@Entity
class Comment : AbstractAuditingEntity() {

  var content: String? = null

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  var article: Article? = null

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  var author: Author? = null
}
