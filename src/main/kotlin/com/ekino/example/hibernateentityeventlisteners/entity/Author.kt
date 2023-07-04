package com.ekino.example.hibernateentityeventlisteners.entity

import javax.persistence.Entity

@Entity
class Author : AbstractAuditingEntity() {

  var name: String? = null

  var age: Int? = null
}
