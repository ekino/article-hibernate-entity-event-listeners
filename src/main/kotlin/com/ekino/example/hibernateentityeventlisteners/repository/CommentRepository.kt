package com.ekino.example.hibernateentityeventlisteners.repository

import com.ekino.example.hibernateentityeventlisteners.entity.Comment
import org.springframework.data.jpa.repository.JpaRepository

interface CommentRepository : JpaRepository<Comment, String>
