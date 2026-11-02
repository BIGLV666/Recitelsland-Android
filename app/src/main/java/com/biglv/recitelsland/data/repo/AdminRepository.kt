package com.biglv.recitelsland.data.repo

import com.biglv.recitelsland.data.model.AdminStats
import com.biglv.recitelsland.data.model.AdminUserDetail
import com.biglv.recitelsland.data.model.AdminUserPage
import com.biglv.recitelsland.data.model.FailedRequestPage
import com.biglv.recitelsland.data.model.FeedbackItem
import com.biglv.recitelsland.data.model.FeedbackPage
import com.biglv.recitelsland.data.model.ImportQuestionResult
import com.biglv.recitelsland.data.model.Question
import com.biglv.recitelsland.data.remote.ApiExecutor
import com.biglv.recitelsland.data.remote.CreateQuestionRequest
import com.biglv.recitelsland.data.remote.ImportQuestionsRequest
import com.biglv.recitelsland.data.remote.ReciteApi
import com.biglv.recitelsland.data.remote.ReplyFeedbackRequest
import com.biglv.recitelsland.data.remote.UpdateRoleRequest
import com.biglv.recitelsland.data.remote.UpdateStatusRequest
import com.biglv.recitelsland.data.remote.toDomain

class AdminRepository(
    private val api: ReciteApi,
    private val executor: ApiExecutor,
) {
    suspend fun stats(): AdminStats =
        executor.unwrapRequired { api.adminStats() }.toDomain()

    suspend fun users(page: Int, limit: Int): AdminUserPage =
        executor.unwrapRequired { api.adminUsers(page, limit) }.toDomain()

    suspend fun userDetail(id: Long): AdminUserDetail =
        executor.unwrapRequired { api.adminUserDetail(id) }.toDomain()

    suspend fun updateRole(id: Long, admin: Boolean) {
        executor.unit {
            api.adminUpdateUserRole(id, UpdateRoleRequest(if (admin) "admin" else "member"))
        }
    }

    suspend fun updateStatus(id: Long, banned: Boolean) {
        executor.unit {
            api.adminUpdateUserStatus(id, UpdateStatusRequest(if (banned) "banned" else "active"))
        }
    }

    suspend fun deleteUser(id: Long) {
        executor.unit { api.adminDeleteUser(id) }
    }

    suspend fun failedRequests(page: Int, limit: Int): FailedRequestPage =
        executor.unwrapRequired { api.adminFailedRequests(page, limit) }.toDomain()

    suspend fun feedbacks(page: Int, limit: Int, status: String?): FeedbackPage =
        executor.unwrapRequired {
            api.adminFeedbacks(page, limit, status?.takeIf { it.isNotBlank() })
        }.toDomain()

    suspend fun replyFeedback(id: Long, reply: String): FeedbackItem =
        executor.unwrapRequired {
            api.adminReplyFeedback(id, ReplyFeedbackRequest(reply))
        }.toDomain()

    suspend fun createQuestion(body: CreateQuestionRequest): Question =
        executor.unwrapRequired { api.adminCreateQuestion(body) }.toDomain()

    suspend fun updateQuestion(id: Long, body: CreateQuestionRequest): Question =
        executor.unwrapRequired { api.adminUpdateQuestion(id, body) }.toDomain()

    suspend fun deleteQuestion(id: Long) {
        executor.unit { api.adminDeleteQuestion(id) }
    }

    suspend fun importQuestions(questions: List<CreateQuestionRequest>): ImportQuestionResult =
        executor.unwrapRequired {
            api.adminImportQuestions(ImportQuestionsRequest(questions))
        }.toDomain()
}

class FeedbackRepository(
    private val api: ReciteApi,
    private val executor: ApiExecutor,
) {
    suspend fun create(
        type: String,
        content: String,
        contact: String,
        appVersion: String,
        platform: String,
    ): FeedbackItem =
        executor.unwrapRequired {
            api.createFeedback(
                com.biglv.recitelsland.data.remote.CreateFeedbackRequest(
                    type = type,
                    content = content,
                    contact = contact,
                    appVersion = appVersion,
                    platform = platform,
                ),
            )
        }.toDomain()

    suspend fun mine(page: Int, limit: Int): FeedbackPage =
        executor.unwrapRequired { api.myFeedbacks(page, limit) }.toDomain()
}
