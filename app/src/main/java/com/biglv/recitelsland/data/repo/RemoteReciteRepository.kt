package com.biglv.recitelsland.data.repo

import com.biglv.recitelsland.data.model.AnswerFilter
import com.biglv.recitelsland.data.model.AnswerRecord
import com.biglv.recitelsland.data.model.AnswerRecordPage
import com.biglv.recitelsland.data.model.FavoriteItem
import com.biglv.recitelsland.data.model.FavoritePage
import com.biglv.recitelsland.data.model.Question
import com.biglv.recitelsland.data.model.QuestionPage
import com.biglv.recitelsland.data.model.ReviewItem
import com.biglv.recitelsland.data.model.ReviewPage
import com.biglv.recitelsland.data.model.TagItem
import com.biglv.recitelsland.data.model.TagPage
import com.biglv.recitelsland.data.model.UserProfile
import com.biglv.recitelsland.data.remote.ApiException
import com.biglv.recitelsland.data.remote.ApiExecutor
import com.biglv.recitelsland.data.remote.ReciteApi
import com.biglv.recitelsland.data.remote.ReviewAnswerRequest
import com.biglv.recitelsland.data.remote.SubmitAnswerRequest
import com.biglv.recitelsland.data.remote.parseServerTime
import com.biglv.recitelsland.data.remote.toDomain
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.ConcurrentHashMap

class RemoteReciteRepository(
    private val api: ReciteApi,
    private val executor: ApiExecutor,
) : ReciteRepository {

    private val questionCache = ConcurrentHashMap<Long, Question>()

    override suspend fun questions(
        cursor: String?,
        limit: Int,
        tag: String?,
        keyword: String,
    ): QuestionPage {
        val data = executor.unwrapRequired {
            api.listQuestions(
                cursor = cursor,
                limit = limit,
                tag = tag?.takeIf { it.isNotBlank() },
                keyword = keyword.trim().takeIf { it.isNotBlank() },
            )
        }
        return QuestionPage(
            items = data.items.map { it.toDomain() },
            nextCursor = data.nextCursor,
            hasMore = data.hasMore,
        )
    }

    override suspend fun question(id: Long): Question? {
        questionCache[id]?.let { return it }
        return try {
            val question = executor.unwrapRequired { api.question(id) }.toDomain()
            questionCache[id] = question
            question
        } catch (e: ApiException) {
            if (e.code / 1000 == 404) null else throw e
        }
    }

    override suspend fun tags(cursor: String?, limit: Int, keyword: String): TagPage {
        val data = executor.unwrapRequired {
            api.tags(
                cursor = cursor,
                limit = limit,
                keyword = keyword.trim().takeIf { it.isNotBlank() },
            )
        }
        return TagPage(
            items = data.items.map { TagItem(name = it.name, questionCount = it.questionCount) },
            nextCursor = data.nextCursor,
            hasMore = data.hasMore,
        )
    }

    override suspend fun submitAnswer(questionId: Long, answers: List<String>) {
        executor.unit { api.submitAnswer(questionId, SubmitAnswerRequest(answers)) }
    }

    override suspend fun completeReading(questionId: Long) {
        executor.unit { api.submitAnswer(questionId, SubmitAnswerRequest(emptyList())) }
    }

    override suspend fun addFavorite(questionId: Long): Long {
        return executor.unwrapRequired { api.addFavorite(questionId) }.id
    }

    override suspend fun deleteFavorite(favoriteId: Long) {
        executor.unit { api.deleteFavorite(favoriteId) }
    }

    override suspend fun favorites(cursor: String?, limit: Int): FavoritePage {
        val data = executor.unwrapRequired { api.favorites(cursor, limit) }
        return FavoritePage(
            items = data.items.map { item ->
                FavoriteItem(
                    favoriteId = item.favoriteId,
                    question = item.question.toDomain(),
                    createdAt = parseServerTime(item.createdAt),
                )
            },
            nextCursor = data.nextCursor,
            hasMore = data.hasMore,
        )
    }

    override suspend fun addReview(questionId: Long) {
        executor.unit { api.addReview(questionId) }
    }

    override suspend fun deleteReview(reviewId: Long) {
        executor.unit { api.deleteReview(reviewId) }
    }

    override suspend fun reviews(cursor: String?, limit: Int): ReviewPage {
        val data = executor.unwrapRequired { api.reviews(cursor, limit) }
        return ReviewPage(
            items = data.items.map { review ->
                ReviewItem(
                    reviewId = review.reviewId,
                    question = review.question.toDomain(),
                    nextReviewAt = parseServerTime(review.nextReviewTime),
                    repetitions = review.repetitions,
                    intervalDays = review.intervalDays,
                )
            },
            nextCursor = data.nextCursor,
            hasMore = data.hasMore,
        )
    }

    override suspend fun todayReviews(): List<ReviewItem> {
        val reviews = executor.unwrapRequired { api.todayReviews() }
        val items = coroutineScope {
            reviews.map { review ->
                async {
                    val question = runCatching { questionOrThrow(review.questionId) }.getOrNull()
                        ?: return@async null
                    ReviewItem(
                        reviewId = review.id,
                        question = question,
                        nextReviewAt = parseServerTime(review.nextReviewTime),
                        repetitions = review.repetitions,
                        intervalDays = review.intervalDays,
                    )
                }
            }.awaitAll()
        }
        return items.filterNotNull()
    }

    override suspend fun submitReview(reviewId: Long, q: Int, answers: List<String>) {
        executor.unwrap {
            api.submitReview(reviewId, ReviewAnswerRequest(q = q, answer = answers))
        }
    }

    override suspend fun answerRecords(
        cursor: String?,
        limit: Int,
        filter: AnswerFilter,
    ): AnswerRecordPage {
        val data = executor.unwrapRequired {
            api.answerRecords(
                cursor = cursor,
                limit = limit,
                trueOrFalse = filter.apiValue,
            )
        }
        return AnswerRecordPage(
            items = data.items.map { record ->
                AnswerRecord(
                    id = record.recordId,
                    question = record.question.toDomain(),
                    answer = record.answer,
                    correct = record.trueOrFalse,
                    createdAt = parseServerTime(record.createdAt),
                )
            },
            nextCursor = data.nextCursor,
            hasMore = data.hasMore,
        )
    }

    override suspend fun answerRecordsForQuestion(questionId: Long): List<AnswerRecord> {
        val question = questionOrThrow(questionId)
        val records = executor.unwrapRequired { api.answerRecordsForQuestion(questionId) }
        return records.map { record ->
            AnswerRecord(
                id = record.id,
                question = question,
                answer = record.answer,
                correct = record.trueOrFalse == 1,
                createdAt = parseServerTime(record.createdAt),
            )
        }
    }

    override suspend fun currentUser(): UserProfile =
        executor.unwrapRequired { api.currentUser() }.toDomain()

    private suspend fun questionOrThrow(id: Long): Question =
        question(id) ?: throw ApiException(404101, "题目不存在")
}
