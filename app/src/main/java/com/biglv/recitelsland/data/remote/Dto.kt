package com.biglv.recitelsland.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val code: Int,
    val msg: String = "",
    val data: T? = null,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class RegisterRequest(
    val username: String,
    val nickname: String = "",
    val email: String,
    val password: String,
    val code: String,
)

@Serializable
data class SendCodeRequest(val email: String)

@Serializable
data class ExchangeCodeRequest(val code: String)

@Serializable
data class LoginResponse(
    val token: String,
    val expiresIn: Long = 0,
    val user: UserDto,
)

@Serializable
data class UserDto(
    val id: Long,
    val username: String = "",
    val nickname: String = "",
    val email: String = "",
    val avatar: String = "",
    val role: Int = 1,
    val allLearnQuestionTotal: Long = 0,
)

@Serializable
data class MiniQuestionDto(
    val id: Long,
    val questionTitle: String = "",
    val questionType: Int = 1,
    val questionPreview: String = "",
    val tags: List<String> = emptyList(),
    val createdAt: String = "",
)

@Serializable
data class ListQuestionsDto(
    val items: List<MiniQuestionDto> = emptyList(),
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
)

@Serializable
data class TagItemDto(
    val id: Long = 0,
    val name: String = "",
    val questionCount: Long = 0,
)

@Serializable
data class TagListDto(
    val items: List<TagItemDto> = emptyList(),
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
)

@Serializable
data class QuestionDto(
    val id: Long,
    val questionTitle: String = "",
    val questionContext: String = "",
    val questionType: Int = 1,
    val answer: List<String> = emptyList(),
    val options: List<String> = emptyList(),
    val explanation: String = "",
    val tags: List<String> = emptyList(),
    val createUserID: Long = 0,
    val createdAt: String = "",
)

@Serializable
data class SubmitAnswerRequest(val answers: List<String> = emptyList())

@Serializable
data class QuestionAnswerRecordDto(
    val id: Long = 0,
    val questionId: Long = 0,
    val answer: List<String> = emptyList(),
    val trueOrFalse: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable
data class MiniAnswerRecordDto(
    val recordId: Long = 0,
    val question: MiniQuestionDto,
    val answer: List<String> = emptyList(),
    val trueOrFalse: Boolean = false,
    val createdAt: String = "",
)

@Serializable
data class ListAnswerRecordsDto(
    val items: List<MiniAnswerRecordDto> = emptyList(),
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
)

@Serializable
data class ReviewDto(
    val id: Long,
    val userId: Long = 0,
    val questionId: Long = 0,
    val easeFactor: Double = 2.5,
    val lastReviewTime: String = "",
    val nextReviewTime: String = "",
    val repetitions: Int = 0,
    val intervalDays: Int = 0,
)

@Serializable
data class MiniReviewDto(
    val reviewId: Long,
    val question: MiniQuestionDto,
    val nextReviewTime: String = "",
    val repetitions: Int = 0,
    val intervalDays: Int = 0,
)

@Serializable
data class ListReviewsDto(
    val items: List<MiniReviewDto> = emptyList(),
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
)

@Serializable
data class ReviewAnswerRequest(
    val q: Int = 0,
    val answer: List<String> = emptyList(),
)

@Serializable
data class FavoriteDto(
    val id: Long = 0,
    val questionId: Long = 0,
)

@Serializable
data class FavoriteItemDto(
    val favoriteId: Long = 0,
    val question: MiniQuestionDto,
    val createdAt: String = "",
)

@Serializable
data class ListFavoritesDto(
    val items: List<FavoriteItemDto> = emptyList(),
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
)


@Serializable
data class UpdateNicknameRequest(val nickname: String)

@Serializable
data class BindEmailCodeRequest(val email: String)

@Serializable
data class BindEmailRequest(
    val email: String,
    val code: String,
)

@Serializable
data class DeleteAccountRequest(val password: String = "")

@Serializable
data class AvatarDto(
    val userId: Long = 0,
    val avatar: String = "",
)

@Serializable
data class AdminUserDto(
    val id: Long = 0,
    val username: String = "",
    val nickname: String = "",
    val email: String = "",
    val avatar: String = "",
    val role: Int = 1,
    val status: String = "active",
    val allLearnQuestionTotal: Long = 0,
    val createdAt: String = "",
)

@Serializable
data class AdminUserListDto(
    val items: List<AdminUserDto> = emptyList(),
    val total: Long = 0,
    val page: Int = 1,
    val limit: Int = 20,
)

@Serializable
data class AdminUserDetailDto(
    val id: Long = 0,
    val username: String = "",
    val nickname: String = "",
    val email: String = "",
    val avatar: String = "",
    val role: Int = 1,
    val status: String = "active",
    val allLearnQuestionTotal: Long = 0,
    val createdAt: String = "",
    val updatedAt: String = "",
    val answerRecords: Long = 0,
    val favorites: Long = 0,
    val reviews: Long = 0,
    val reviewRecords: Long = 0,
)

@Serializable
data class UpdateRoleRequest(val role: String)

@Serializable
data class UpdateStatusRequest(val status: String)

@Serializable
data class AdminStatsDto(
    val users: Long = 0,
    val questions: Long = 0,
    val answerRecords: Long = 0,
    val reviews: Long = 0,
    val reviewRecords: Long = 0,
    val favorites: Long = 0,
    val feedbacks: Long = 0,
    val pendingFeedbacks: Long = 0,
)

@Serializable
data class FailedRequestDto(
    val requestId: String = "",
    val time: String = "",
    val method: String = "",
    val path: String = "",
    val status: Int = 0,
    val code: Int = 0,
    val message: String = "",
    val ip: String = "",
    val userId: Long = 0,
    val latencyMs: Double = 0.0,
)

@Serializable
data class FailedRequestListDto(
    val items: List<FailedRequestDto> = emptyList(),
    val total: Long = 0,
    val page: Int = 1,
    val limit: Int = 20,
)

@Serializable
data class CreateFeedbackRequest(
    val type: String,
    val content: String,
    val contact: String = "",
    val appVersion: String = "",
    val platform: String = "android",
)

@Serializable
data class ReplyFeedbackRequest(val reply: String)

@Serializable
data class FeedbackDto(
    val id: Long = 0,
    val type: String = "",
    val content: String = "",
    val contact: String = "",
    val appVersion: String = "",
    val platform: String = "",
    val status: String = "",
    val adminReply: String = "",
    val repliedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val userId: Long = 0,
    val userNickname: String = "",
    val userEmail: String = "",
)

@Serializable
data class FeedbackListDto(
    val items: List<FeedbackDto> = emptyList(),
    val total: Long = 0,
    val page: Int = 1,
    val limit: Int = 20,
)

@Serializable
data class CreateQuestionRequest(
    val questionTitle: String,
    val questionContext: String,
    val questionType: Int,
    val answer: List<String> = emptyList(),
    val options: List<String> = emptyList(),
    val explanation: String,
    val tags: List<String> = emptyList(),
)

@Serializable
data class ImportQuestionsRequest(val questions: List<CreateQuestionRequest>)

@Serializable
data class ImportFailureDto(
    val index: Int = 0,
    val reason: String = "",
)

@Serializable
data class ImportQuestionsResultDto(
    val total: Int = 0,
    val imported: Int = 0,
    val skipped: Int = 0,
    val failed: Int = 0,
    val errors: List<ImportFailureDto> = emptyList(),
)
