package com.biglv.recitelsland.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthDtoTest {

    @Test
    fun loginResponse_matchesBackendShape() {
        val raw = """
            {"code":0,"msg":"ok","data":{"token":"abc123","expiresIn":604800,
             "user":{"id":2,"username":"rtest","nickname":"测试岛民",
             "email":"rtest@example.com","avatar":"/uploads/avatars/a.png",
             "role":1,"allLearnQuestionTotal":12}}}
        """.trimIndent()

        val response = ApiClient.json.decodeFromString<ApiResponse<LoginResponse>>(raw)
        assertEquals(0, response.code)
        val data = response.data!!
        assertEquals("abc123", data.token)
        assertEquals(604800, data.expiresIn)
        val user = data.user.toDomain()
        assertEquals("测试岛民", user.nickname)
        assertEquals("普通用户", user.role)
        assertEquals(12, user.learnedQuestionTotal)
        assertTrue(user.avatar.endsWith("/uploads/avatars/a.png"))
        assertTrue(user.avatar.startsWith("https://"))
    }

    @Test
    fun errorResponse_keepsBusinessMessage() {
        val raw = """{"code":401002,"msg":"邮箱或密码错误"}"""
        val response = ApiClient.json.decodeFromString<ApiResponse<Unit>>(raw)
        assertEquals(401002, response.code)
        assertEquals("邮箱或密码错误", response.msg)
    }
}
