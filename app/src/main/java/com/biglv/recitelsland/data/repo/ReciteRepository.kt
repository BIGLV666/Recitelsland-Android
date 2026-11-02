package com.biglv.recitelsland.data.repo

import com.biglv.recitelsland.data.model.AnswerFilter
import com.biglv.recitelsland.data.model.AnswerRecord
import com.biglv.recitelsland.data.model.AnswerRecordPage
import com.biglv.recitelsland.data.model.FavoritePage
import com.biglv.recitelsland.data.model.Question
import com.biglv.recitelsland.data.model.QuestionPage
import com.biglv.recitelsland.data.model.ReviewItem
import com.biglv.recitelsland.data.model.ReviewPage
import com.biglv.recitelsland.data.model.TagPage
import com.biglv.recitelsland.data.model.UserProfile

/**
 * 页面只依赖这个接口，当前实现为 RemoteReciteRepository。
 */
interface ReciteRepository {
    suspend fun questions(
        cursor: String?,
        limit: Int,
        tag: String?,
        keyword: String,
    ): QuestionPage

    suspend fun question(id: Long): Question?

    suspend fun tags(cursor: String?, limit: Int, keyword: String): TagPage

    suspend fun submitAnswer(questionId: Long, answers: List<String>)

    suspend fun completeReading(questionId: Long)

    suspend fun addFavorite(questionId: Long): Long

    suspend fun deleteFavorite(favoriteId: Long)

    suspend fun favorites(cursor: String?, limit: Int): FavoritePage

    suspend fun addReview(questionId: Long)

    suspend fun deleteReview(reviewId: Long)

    suspend fun reviews(cursor: String?, limit: Int): ReviewPage

    suspend fun todayReviews(): List<ReviewItem>

    suspend fun submitReview(reviewId: Long, q: Int, answers: List<String>)

    suspend fun answerRecords(cursor: String?, limit: Int, filter: AnswerFilter): AnswerRecordPage

    suspend fun answerRecordsForQuestion(questionId: Long): List<AnswerRecord>

    suspend fun currentUser(): UserProfile
}
