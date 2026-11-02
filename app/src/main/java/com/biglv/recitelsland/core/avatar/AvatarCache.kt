package com.biglv.recitelsland.core.avatar

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 头像本地缓存。
 *
 * 每个用户一份文件 + 一份远程 URL 元数据：
 * - URL 没变：直接读本地文件，不再请求后端。
 * - URL 变了（用户在别的设备换了头像）：重新下载并覆盖。
 * - 上传头像成功：直接用刚上传的字节写缓存，立即可见。
 */
class AvatarCache(context: Context) {

    private val dir = File(context.filesDir, "avatars").apply { mkdirs() }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun fileOf(userId: Long): File = File(dir, "$userId.img")

    suspend fun cached(userId: Long): File? = withContext(Dispatchers.IO) {
        fileOf(userId).takeIf { it.exists() && it.length() > 0 }
    }

    suspend fun ensure(userId: Long, remoteUrl: String?): File? = withContext(Dispatchers.IO) {
        if (userId <= 0) return@withContext null
        val file = fileOf(userId)
        val meta = File(dir, "$userId.url")

        if (remoteUrl.isNullOrBlank()) {
            file.delete()
            meta.delete()
            return@withContext null
        }

        if (file.exists() && file.length() > 0 && meta.exists() && meta.readText() == remoteUrl) {
            return@withContext file
        }

        try {
            val request = Request.Builder().url(remoteUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext file.takeIf { it.exists() && it.length() > 0 }
                }
                val bytes = response.body?.bytes()
                    ?: return@withContext file.takeIf { it.exists() && it.length() > 0 }
                val tmp = File(dir, "$userId.tmp")
                tmp.writeBytes(bytes)
                if (!tmp.renameTo(file)) {
                    file.writeBytes(bytes)
                    tmp.delete()
                }
                meta.writeText(remoteUrl)
                file
            }
        } catch (_: Exception) {
            file.takeIf { it.exists() && it.length() > 0 }
        }
    }

    suspend fun save(userId: Long, remoteUrl: String, bytes: ByteArray): File =
        withContext(Dispatchers.IO) {
            val file = fileOf(userId)
            val tmp = File(dir, "$userId.tmp")
            tmp.writeBytes(bytes)
            if (!tmp.renameTo(file)) {
                file.writeBytes(bytes)
                tmp.delete()
            }
            File(dir, "$userId.url").writeText(remoteUrl)
            file
        }

    suspend fun delete(userId: Long) = withContext(Dispatchers.IO) {
        fileOf(userId).delete()
        File(dir, "$userId.url").delete()
    }
}
