package com.ekino.example.hibernateentityeventlisteners.repository

import com.ekino.example.hibernateentityeventlisteners.entity.Category
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryRepository : JpaRepository<Category, String>
