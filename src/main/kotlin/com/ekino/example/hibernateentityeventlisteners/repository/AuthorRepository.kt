package com.ekino.example.hibernateentityeventlisteners.repository

import com.ekino.example.hibernateentityeventlisteners.entity.Author
import org.springframework.data.jpa.repository.JpaRepository

interface AuthorRepository : JpaRepository<Author, String>
