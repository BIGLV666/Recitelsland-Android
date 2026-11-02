package com.biglv.recitelsland.data.model

import java.time.LocalDateTime

data class AdminUser(
    val id: Long,
    val username: String,
    val nickname: String,
    val email: String,
    val avatar: String,
    val role: Int,
    val status: String,
    val learnedQuestionTotal: Long,
    val createdAt: LocalDateTime,
) {
    val isAdmin: Boolean get() = role == 2
    val isBanned: Boolean get() = status == "banned"
}

data class AdminUserPage(
    val items: List<AdminUser>,
    val total: Long,
    val page: Int,
    val limit: Int,
)

data class AdminUserDetail(
    val user: AdminUser,
    val answerRecords: Long,
    val favorites: Long,
    val reviews: Long,
    val reviewRecords: Long,
)

data class AdminStats(
    val users: Long,
    val questions: Long,
    val answerRecords: Long,
    val reviews: Long,
    val reviewRecords: Long,
    val favorites: Long,
    val feedbacks: Long,
    val pendingFeedbacks: Long,
)

data class FailedRequest(
    val requestId: String,
    val time: LocalDateTime,
    val method: String,
    val path: String,
    val status: Int,
    val code: Int,
    val message: String,
    val ip: String,
    val userId: Long,
    val latencyMs: Double,
)

data class FailedRequestPage(
    val items: List<FailedRequest>,
    val total: Long,
    val page: Int,
    val limit: Int,
)

data class FeedbackItem(
    val id: Long,
    val type: String,
    val content: String,
    val contact: String,
    val appVersion: String,
    val platform: String,
    val status: String,
    val adminReply: String,
    val createdAt: LocalDateTime,
    val userId: Long = 0,
    val userNickname: String = "",
    val userEmail: String = "",
) {
    val isPending: Boolean get() = status == "pending"
}

data class FeedbackPage(
    val items: List<FeedbackItem>,
    val total: Long,
    val page: Int,
    val limit: Int,
)

data class ImportQuestionResult(
    val total: Int,
    val imported: Int,
    val skipped: Int,
    val failed: Int,
    val errors: List<ImportQuestionFailure>,
)

data class ImportQuestionFailure(
    val index: Int,
    val reason: String,
)
