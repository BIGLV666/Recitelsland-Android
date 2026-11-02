package com.biglv.recitelsland.data.repo

import com.biglv.recitelsland.data.remote.ApiClient
import com.biglv.recitelsland.data.remote.ApiExecutor
import com.biglv.recitelsland.data.remote.ReciteApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class RemoteReciteRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: RemoteReciteRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(ApiClient.json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ReciteApi::class.java)
        repository = RemoteReciteRepository(api, ApiExecutor(ApiClient.json))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun questions_mapsCursorAndSummaryFields() = runBlocking {
        enqueue(
            """
            {"code":0,"msg":"ok","data":{"items":[
              {"id":1,"questionTitle":"TCP","questionType":1,"questionPreview":"preview",
               "tags":["网络"],"createdAt":"2026-11-01T10:00:00+08:00"}
            ],"hasMore":true,"nextCursor":"abc"}}
            """.trimIndent(),
        )

        val page = repository.questions(cursor = null, limit = 20, tag = null, keyword = "")
        assertEquals(1, page.items.size)
        assertEquals("TCP", page.items.first().title)
        assertEquals("preview", page.items.first().preview)
        assertEquals(listOf("网络"), page.items.first().tags)
        assertEquals("abc", page.nextCursor)
        assertTrue(page.hasMore)
    }

    @Test
    fun tags_mapsNames() = runBlocking {
        enqueue(
            """
            {"code":0,"msg":"ok","data":{"items":[
              {"id":1,"name":"Go","questionCount":12},{"id":2,"name":"网络","questionCount":3}
            ],"hasMore":false}}
            """.trimIndent(),
        )

        val page = repository.tags(cursor = null, limit = 50, keyword = "")
        assertEquals(listOf("Go", "网络"), page.items.map { it.name })
        assertEquals(12, page.items.first().questionCount)
        assertFalse(page.hasMore)
    }

    @Test
    fun answerRecords_mapsBooleanResult() = runBlocking {
        enqueue(
            """
            {"code":0,"msg":"ok","data":{"items":[
              {"recordId":7,"question":{"id":1,"questionTitle":"TCP","questionType":1,
               "questionPreview":"preview","tags":[],"createdAt":"2026-11-01T10:00:00+08:00"},
               "answer":["A"],"trueOrFalse":true,"createdAt":"2026-11-01T11:00:00+08:00"}
            ],"nextCursor":"next","hasMore":true}}
            """.trimIndent(),
        )

        val page = repository.answerRecords(
            cursor = null,
            limit = 20,
            filter = com.biglv.recitelsland.data.model.AnswerFilter.All,
        )
        assertEquals(1, page.items.size)
        assertEquals("next", page.nextCursor)
        assertTrue(page.hasMore)
        assertEquals(7L, page.items.first().id)
        assertTrue(page.items.first().correct)
        assertEquals("TCP", page.items.first().question.title)
        assertEquals(11, page.items.first().createdAt.hour)
    }

    @Test
    fun todayReviews_loadsQuestionDetail() = runBlocking {
        enqueue(
            """
            {"code":0,"msg":"ok","data":[
              {"id":9,"userId":2,"questionId":1,"easeFactor":2.5,
               "lastReviewTime":"2026-10-31T10:00:00+08:00",
               "nextReviewTime":"2026-11-01T10:00:00+08:00",
               "repetitions":2,"intervalDays":6}
            ]}
            """.trimIndent(),
        )
        enqueue(
            """
            {"code":0,"msg":"ok","data":{"id":1,"questionTitle":"TCP","questionContext":"context",
             "questionType":1,"answer":["A"],"options":["A","B"],"explanation":"因为",
             "tags":["网络"],"createUserID":1,"createdAt":"2026-11-01T10:00:00+08:00"}}
            """.trimIndent(),
        )

        val reviews = repository.todayReviews()
        assertEquals(1, reviews.size)
        assertEquals(9L, reviews.first().reviewId)
        assertEquals(1L, reviews.first().question.id)
        assertEquals("context", reviews.first().question.context)
        assertEquals(2, reviews.first().repetitions)
    }

    @Test
    fun questionRecords_mapsIntResult() = runBlocking {
        enqueue(
            """
            {"code":0,"msg":"ok","data":{"id":1,"questionTitle":"TCP","questionContext":"context",
             "questionType":2,"answer":["A"],"options":["正确","错误"],"explanation":"因为",
             "tags":[],"createUserID":1,"createdAt":"2026-11-01T10:00:00+08:00"}}
            """.trimIndent(),
        )
        enqueue(
            """
            {"code":0,"msg":"ok","data":[
              {"id":11,"questionId":1,"answer":["A"],"trueOrFalse":1,
               "createdAt":"2026-11-01T11:00:00+08:00"}
            ]}
            """.trimIndent(),
        )

        val records = repository.answerRecordsForQuestion(1)
        assertEquals(1, records.size)
        assertTrue(records.first().correct)
        assertEquals("context", records.first().question.context)
    }

    @Test
    fun submitAnswer_sendsAnswersToCorrectPath() = runBlocking {
        enqueue("""{"code":0,"msg":"作答已提交"}""")

        repository.submitAnswer(5, listOf("A", "B"))

        val request = server.takeRequest()
        assertEquals("/questions/5/answer-records", request.path)
        assertTrue(request.body.readUtf8().contains("\"A\""))
    }


    @Test
    fun favorites_mapsFavoriteIdAndQuestion() = runBlocking {
        enqueue(
            """
            {"code":0,"msg":"ok","data":{"items":[
              {"favoriteId":3,"createdAt":"2026-11-01T12:00:00+08:00",
               "question":{"id":1,"questionTitle":"TCP","questionType":1,
               "questionPreview":"preview","tags":["网络"],"createdAt":"2026-11-01T10:00:00+08:00"}}
            ],"hasMore":false}}
            """.trimIndent(),
        )

        val page = repository.favorites(cursor = null, limit = 20)
        assertEquals(1, page.items.size)
        assertEquals(3L, page.items.first().favoriteId)
        assertEquals("TCP", page.items.first().question.title)
    }

    @Test
    fun reviewPlans_mapsScheduleAndQuestion() = runBlocking {
        enqueue(
            """
            {"code":0,"msg":"ok","data":{"items":[
              {"reviewId":9,"question":{"id":1,"questionTitle":"TCP","questionType":1,
               "questionPreview":"preview","tags":[],"createdAt":"2026-11-01T10:00:00+08:00"},
               "nextReviewTime":"2026-11-02T10:00:00+08:00","repetitions":2,"intervalDays":6}
            ],"hasMore":false}}
            """.trimIndent(),
        )

        val page = repository.reviews(cursor = null, limit = 20)
        assertEquals(1, page.items.size)
        assertEquals(9L, page.items.first().reviewId)
        assertEquals(2, page.items.first().repetitions)
        assertEquals(6, page.items.first().intervalDays)
    }

    @Test
    fun deleteFavorite_andDeleteReview_useDeletePaths() = runBlocking {
        enqueue("""{"code":0,"msg":"已取消收藏"}""")
        enqueue("""{"code":0,"msg":"已移出复习计划"}""")

        repository.deleteFavorite(3)
        repository.deleteReview(9)

        assertEquals("/favorites/3", server.takeRequest().path)
        assertEquals("/reviews/9", server.takeRequest().path)
    }

    private fun enqueue(body: String) {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(body),
        )
    }
}
