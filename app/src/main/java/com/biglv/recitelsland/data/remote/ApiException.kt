package com.biglv.recitelsland.data.remote

import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

class ApiException(
    val code: Int,
    override val message: String,
) : Exception(message) {
    val isUnauthorized: Boolean get() = code == 401001 || code / 1000 == 401
}

/**
 * 把后端统一响应体展开成 data；code != 0 或 HTTP 错误统一转成 ApiException。
 *
 * 401 会额外触发 [onUnauthorized]，让根界面回到登录页。
 */
class ApiExecutor(
    private val json: Json = ApiClient.json,
    private val onUnauthorized: suspend () -> Unit = {},
) {
    suspend fun <T> unwrap(block: suspend () -> ApiResponse<T>): T? {
        val response = try {
            block()
        } catch (e: HttpException) {
            throw toApiException(e)
        } catch (e: IOException) {
            throw ApiException(-1, "网络连接失败，请检查网络后重试")
        }

        if (response.code != 0) {
            if (response.code / 1000 == 401) onUnauthorized()
            throw ApiException(response.code, response.msg.ifBlank { "请求失败" })
        }
        return response.data
    }

    suspend fun <T> unwrapRequired(block: suspend () -> ApiResponse<T>): T =
        unwrap(block) ?: throw ApiException(-2, "服务器返回数据为空")

    suspend fun unit(block: suspend () -> ApiResponse<Unit>) {
        unwrap(block)
    }

    private fun toApiException(e: HttpException): ApiException {
        val raw = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        if (!raw.isNullOrBlank()) {
            val parsed = runCatching {
                json.decodeFromString<ApiResponse<kotlinx.serialization.json.JsonElement>>(raw)
            }.getOrNull()
            if (parsed != null) {
                return ApiException(parsed.code, parsed.msg.ifBlank { "请求失败" })
            }
        }
        return when (e.code()) {
            401 -> ApiException(401001, "登录已过期，请重新登录")
            403 -> ApiException(403001, "没有权限执行该操作")
            404 -> ApiException(404000, "请求的内容不存在")
            429 -> ApiException(429001, "请求过于频繁，请稍后重试")
            else -> ApiException(e.code(), "服务器返回错误（${e.code()}）")
        }
    }
}
