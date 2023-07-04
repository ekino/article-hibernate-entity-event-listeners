package com.ekino.example.hibernateentityeventlisteners.entity

import javax.persistence.Entity

@Entity
class Category : AbstractAuditingEntity() {

  var name: String? = null
}
