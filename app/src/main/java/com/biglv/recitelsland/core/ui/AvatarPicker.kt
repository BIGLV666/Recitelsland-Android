package com.biglv.recitelsland.core.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * 读取相册图片，压缩到适合头像的大小，避免直接把几 MB 的原图传给后端。
 * 返回 JPEG 字节和 MIME；后端允许 image/jpeg。
 */
suspend fun readAvatarBytes(context: Context, uri: Uri): Pair<ByteArray, String>? =
    withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, boundsOptions)
            }
            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return@runCatching null

            var sampleSize = 1
            while (boundsOptions.outWidth / (sampleSize * 2) >= MAX_SIZE &&
                boundsOptions.outHeight / (sampleSize * 2) >= MAX_SIZE
            ) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            } ?: return@runCatching null

            val scaled = scaleDown(decoded)
            val output = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 88, output)
            if (scaled !== decoded) scaled.recycle()
            decoded.recycle()
            output.toByteArray() to "image/jpeg"
        }.getOrNull()
    }

private fun scaleDown(bitmap: Bitmap): Bitmap {
    val maxSide = maxOf(bitmap.width, bitmap.height)
    if (maxSide <= MAX_SIZE) return bitmap
    val ratio = MAX_SIZE.toFloat() / maxSide
    return Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * ratio).toInt().coerceAtLeast(1),
        (bitmap.height * ratio).toInt().coerceAtLeast(1),
        true,
    )
}

private const val MAX_SIZE = 1024
