package com.ekino.example.hibernateentityeventlisteners.listener.hibernate

import com.ekino.example.hibernateentityeventlisteners.AbstractIntegrationTest
import com.ekino.example.hibernateentityeventlisteners.EntityId
import com.ekino.example.hibernateentityeventlisteners.entity.Article
import com.ekino.example.hibernateentityeventlisteners.entity.Author
import com.ekino.example.hibernateentityeventlisteners.entity.Category
import com.ekino.example.hibernateentityeventlisteners.entity.Comment
import com.ekino.example.hibernateentityeventlisteners.entityId
import com.ekino.example.hibernateentityeventlisteners.repository.ArticleRepository
import com.ekino.example.hibernateentityeventlisteners.repository.AuthorRepository
import com.ekino.example.hibernateentityeventlisteners.repository.CategoryRepository
import com.ekino.example.hibernateentityeventlisteners.repository.CommentRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class HibernateEntityEventListenerTest : AbstractIntegrationTest() {

  override val logger = KotlinLogging.logger {}

  @Autowired
  lateinit var categoryRepository: CategoryRepository

  @Autowired
  lateinit var authorRepository: AuthorRepository

  @Autowired
  lateinit var articleRepository: ArticleRepository

  @Autowired
  lateinit var commentRepository: CommentRepository

  @BeforeEach
  fun setUp() {
    commentRepository.deleteAll()
    articleRepository.deleteAll()
    categoryRepository.deleteAll()
    authorRepository.deleteAll()
  }

  @Test
  fun `should trigger entity events`() {
    lateinit var kotlinCategoryId: EntityId<Category>
    lateinit var hibernateCategoryId: EntityId<Category>
    lateinit var authorId: EntityId<Author>
    lateinit var articleId: EntityId<Article>
    lateinit var commentId: EntityId<Comment>
    inTransaction(title = "Data initialization") {
      val (
        hibernateCategory,
        eventCategory,
        kotlinCategory,
      ) = sequenceOf(
        "Hibernate",
        "Event",
        "Kotlin",
      )
        .map {
          Category().apply {
            name = it
          }
        }
        .map(categoryRepository::save)
        .toList()
      kotlinCategoryId = kotlinCategory.entityId()
      hibernateCategoryId = hibernateCategory.entityId()

      val leo = Author().apply {
        name = "Léo"
      }
        .let(authorRepository::save)
        .also { authorId = it.entityId() }

      val article = Article().apply {
        title = "Awesome entity event listeners!"
        content = "Some article content..."
        author = leo
        categories.apply {
          add(hibernateCategory)
          add(eventCategory)
        }
      }
        .let(articleRepository::save)
        .also { articleId = it.entityId() }

      Comment().apply {
        content = "Some nice comment ;)"
        this.author = leo
        this.article = article
      }
        .let(commentRepository::save)
        .also { commentId = it.entityId() }
    }

    inTransaction(title = "Multiple saves") {
      // multiple saves in the same transaction should trigger only one update event
      authorId.find()
        .apply {
          age = 18
        }
        .let(authorRepository::save)
        .apply {
          age = 34
        }
        .let(authorRepository::save)
    }

    inTransaction(title = "Save without modification") {
      authorId.find()
        .let(authorRepository::save)
    }

    inTransaction(title = "Omitted save call") {
      // save call on repository is optional
      authorId.find()
        .apply {
          name = "Léo Millon"
        }
    }

    inTransaction(title = "Modification on existing set") {
      articleId.find()
        .apply {
          categories.apply {
            clear()
            add(kotlinCategoryId.reference())
          }
        }
    }

    inTransaction(title = "Modification on existing set and on simple property") {
      articleId.find()
        .apply {
          content = "Some fixed article content..."
          categories.apply {
            clear()
            add(hibernateCategoryId.reference())
          }
        }
    }

    inTransaction(title = "Modification with new empty set") {
      articleId.find()
        .apply {
          categories = mutableSetOf()
        }
    }

    inTransaction(title = "Modification using new set") {
      articleId.find()
        .apply {
          categories = mutableSetOf(kotlinCategoryId.reference())
        }
    }

    inTransaction(title = "Article deletion") {
      commentRepository.delete(commentId.reference())
      articleRepository.delete(articleId.reference())
    }
  }
}
