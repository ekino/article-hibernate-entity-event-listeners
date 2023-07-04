package com.ekino.example.hibernateentityeventlisteners.repository

import com.ekino.example.hibernateentityeventlisteners.entity.Article
import org.springframework.data.jpa.repository.JpaRepository

interface ArticleRepository : JpaRepository<Article, String>
