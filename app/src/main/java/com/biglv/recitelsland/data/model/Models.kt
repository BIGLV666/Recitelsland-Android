package com.biglv.recitelsland.data.model

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

enum class QuestionType(val apiValue: Int, val label: String) {
    MultipleChoice(1, "选择题"),
    TrueFalse(2, "判断题"),
    Reading(3, "阅读题");

    companion object {
        fun fromApi(value: Int): QuestionType =
            entries.firstOrNull { it.apiValue == value } ?: MultipleChoice
    }
}

data class Question(
    val id: Long,
    val title: String,
    val context: String,
    val type: QuestionType,
    val options: List<String> = emptyList(),
    val answer: List<String> = emptyList(),
    val explanation: String,
    val tags: List<String> = emptyList(),
    val preview: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    val supportsAnswerReveal: Boolean get() = type != QuestionType.Reading
}

data class AnswerRecord(
    val id: Long,
    val question: Question,
    val answer: List<String>,
    val correct: Boolean,
    val createdAt: LocalDateTime,
)

data class AnswerRecordPage(
    val items: List<AnswerRecord>,
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
)

data class ReviewItem(
    val reviewId: Long,
    val question: Question,
    val nextReviewAt: LocalDateTime,
    val repetitions: Int,
    val intervalDays: Int,
) {
    val isOverdue: Boolean get() = nextReviewAt.toLocalDate().isBefore(LocalDateTime.now().toLocalDate())
}

@Serializable
data class UserProfile(
    val id: Long,
    val username: String,
    val nickname: String,
    val email: String,
    val role: String,
    val learnedQuestionTotal: Long,
    val avatar: String = "",
) {
    val isAdmin: Boolean get() = role == "管理员"
}

data class QuestionPage(
    val items: List<Question>,
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
)

data class FavoriteItem(
    val favoriteId: Long,
    val question: Question,
    val createdAt: LocalDateTime,
)

data class FavoritePage(
    val items: List<FavoriteItem>,
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
)

data class ReviewPage(
    val items: List<ReviewItem>,
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
)

/**
 * 标签分页结果。后端标签接口后续会按游标分页返回；
 * 当前 Demo 一次返回全部标签，hasMore = false。
 */
data class TagItem(
    val name: String,
    val questionCount: Long = 0,
)

data class TagPage(
    val items: List<TagItem>,
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
)

enum class AnswerFilter(val label: String, val apiValue: Int) {
    All("全部", 0),
    Correct("答对", 1),
    Incorrect("答错", 2),
}

enum class QuestionFilter(val label: String) {
    All("全部"),
    MultipleChoice("选择"),
    TrueFalse("判断"),
    Reading("阅读"),
}
