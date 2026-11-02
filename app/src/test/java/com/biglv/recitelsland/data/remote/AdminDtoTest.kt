package com.biglv.recitelsland.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminDtoTest {

    @Test
    fun adminUsersMatchesBackendShape() {
        val raw = """
            {"code":0,"msg":"ok","data":{"items":[{"id":2,"username":"u","nickname":"n",
             "email":"u@example.com","avatar":"","role":2,"status":"active",
             "allLearnQuestionTotal":8,"createdAt":"2026-11-01T10:00:00+08:00"}],
             "total":1,"page":1,"limit":20}}
        """.trimIndent()
        val page = ApiClient.json.decodeFromString<ApiResponse<AdminUserListDto>>(raw).data!!
        val user = page.items.first().toDomain()
        assertEquals(2L, user.id)
        assertTrue(user.isAdmin)
        assertEquals("active", user.status)
        assertEquals(8, user.learnedQuestionTotal)
    }

    @Test
    fun adminFeedbackKeepsSubmitterFields() {
        val raw = """
            {"code":0,"msg":"ok","data":{"items":[{"id":5,"type":"bug","content":"x",
             "status":"pending","adminReply":"","createdAt":"2026-11-01T10:00:00+08:00",
             "userId":2,"userNickname":"测试","userEmail":"t@example.com"}],
             "total":1,"page":1,"limit":20}}
        """.trimIndent()
        val page = ApiClient.json.decodeFromString<ApiResponse<FeedbackListDto>>(raw).data!!
        val item = page.items.first().toDomain()
        assertEquals("bug", item.type)
        assertEquals("测试", item.userNickname)
        assertTrue(item.isPending)
    }

    @Test
    fun feedbackRequestContainsVersionAndDevice() {
        val raw = ApiClient.json.encodeToString(
            CreateFeedbackRequest(
                type = "bug",
                content = "x",
                appVersion = "0.4.0",
                platform = "android vivo V2463A",
            ),
        )
        assertTrue(raw.contains("\"appVersion\":\"0.4.0\""))
        assertTrue(raw.contains("android vivo V2463A"))
    }
}
