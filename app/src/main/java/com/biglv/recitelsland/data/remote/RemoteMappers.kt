package com.biglv.recitelsland.data.remote

import com.biglv.recitelsland.data.model.Question
import com.biglv.recitelsland.data.model.QuestionType
import com.biglv.recitelsland.data.model.UserProfile
import java.time.LocalDateTime
import java.time.OffsetDateTime

fun UserDto.toDomain(): UserProfile = UserProfile(
    id = id,
    username = username,
    nickname = nickname.ifBlank { username },
    email = email,
    role = if (role == 2) "管理员" else "普通用户",
    learnedQuestionTotal = allLearnQuestionTotal,
    avatar = ApiConfig.absoluteUrl(avatar).orEmpty(),
)

fun MiniQuestionDto.toDomain(): Question = Question(
    id = id,
    title = questionTitle,
    context = "",
    type = QuestionType.fromApi(questionType),
    explanation = "",
    tags = tags,
    preview = questionPreview,
    createdAt = parseServerTime(createdAt),
)

fun QuestionDto.toDomain(): Question = Question(
    id = id,
    title = questionTitle,
    context = questionContext,
    type = QuestionType.fromApi(questionType),
    options = options,
    answer = answer,
    explanation = explanation,
    tags = tags,
    preview = questionPreview(questionContext),
    createdAt = parseServerTime(createdAt),
)

private fun previewText(text: String, limit: Int = 92): String {
    val flat = text.replace(Regex("\\s+"), " ").trim()
    return if (flat.length <= limit) flat else flat.take(limit) + "…"
}

private fun questionPreview(text: String): String = previewText(text)

fun parseServerTime(raw: String, fallback: LocalDateTime = LocalDateTime.now()): LocalDateTime {
    if (raw.isBlank()) return fallback
    return runCatching {
        OffsetDateTime.parse(raw).toLocalDateTime()
    }.getOrElse {
        runCatching { LocalDateTime.parse(raw) }.getOrDefault(fallback)
    }
}
