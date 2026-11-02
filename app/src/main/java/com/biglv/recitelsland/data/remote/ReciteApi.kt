package com.biglv.recitelsland.data.remote

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.PUT

/**
 * 后端接口声明。统一响应体：{ code, msg, data }。
 *
 * 生产地址：https://recitelsland.biglv.asia/api/v1/
 */
interface ReciteApi {

    // ---------- 公开接口 ----------

    @POST("users/verify-code")
    suspend fun sendCode(@Body body: SendCodeRequest): ApiResponse<Unit>

    @POST("users")
    suspend fun register(@Body body: RegisterRequest): ApiResponse<LoginResponse>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): ApiResponse<LoginResponse>

    @POST("auth/github/exchange")
    suspend fun exchangeGithubCode(@Body body: ExchangeCodeRequest): ApiResponse<LoginResponse>

    // ---------- 登录后接口 ----------

    @GET("users/me")
    suspend fun currentUser(): ApiResponse<UserDto>

    @PUT("users/me/nickname")
    suspend fun updateNickname(@Body body: UpdateNicknameRequest): ApiResponse<UserDto>

    @POST("users/me/email/verify-code")
    suspend fun sendBindEmailCode(@Body body: BindEmailCodeRequest): ApiResponse<Unit>

    @PUT("users/me/email")
    suspend fun bindEmail(@Body body: BindEmailRequest): ApiResponse<UserDto>

    @POST("auth/logout")
    suspend fun logout(): ApiResponse<Unit>

    @Multipart
    @POST("users/me/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): ApiResponse<UserDto>

    @GET("users/me/avatar")
    suspend fun myAvatar(): ApiResponse<AvatarDto>

    @GET("users/{id}/avatar")
    suspend fun userAvatar(@Path("id") id: Long): ApiResponse<AvatarDto>

    @DELETE("users/me/avatar")
    suspend fun deleteAvatar(): ApiResponse<UserDto>

    @HTTP(method = "DELETE", path = "users/me", hasBody = true)
    suspend fun deleteAccount(@Body body: DeleteAccountRequest): ApiResponse<Unit>

    // ---------- 反馈 ----------

    @POST("feedbacks")
    suspend fun createFeedback(@Body body: CreateFeedbackRequest): ApiResponse<FeedbackDto>

    @GET("feedbacks")
    suspend fun myFeedbacks(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): ApiResponse<FeedbackListDto>

    // ---------- 管理端 ----------

    @GET("admin/stats")
    suspend fun adminStats(): ApiResponse<AdminStatsDto>

    @GET("admin/users")
    suspend fun adminUsers(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): ApiResponse<AdminUserListDto>

    @GET("admin/users/{id}")
    suspend fun adminUserDetail(@Path("id") id: Long): ApiResponse<AdminUserDetailDto>

    @PUT("admin/users/{id}/role")
    suspend fun adminUpdateUserRole(
        @Path("id") id: Long,
        @Body body: UpdateRoleRequest,
    ): ApiResponse<Unit>

    @PUT("admin/users/{id}/status")
    suspend fun adminUpdateUserStatus(
        @Path("id") id: Long,
        @Body body: UpdateStatusRequest,
    ): ApiResponse<Unit>

    @DELETE("admin/users/{id}")
    suspend fun adminDeleteUser(@Path("id") id: Long): ApiResponse<Unit>

    @GET("admin/failed-requests")
    suspend fun adminFailedRequests(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): ApiResponse<FailedRequestListDto>

    @GET("admin/feedbacks")
    suspend fun adminFeedbacks(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("status") status: String? = null,
    ): ApiResponse<FeedbackListDto>

    @POST("admin/feedbacks/{id}/reply")
    suspend fun adminReplyFeedback(
        @Path("id") id: Long,
        @Body body: ReplyFeedbackRequest,
    ): ApiResponse<FeedbackDto>

    @POST("questions")
    suspend fun adminCreateQuestion(@Body body: CreateQuestionRequest): ApiResponse<QuestionDto>

    @PUT("questions/{id}")
    suspend fun adminUpdateQuestion(
        @Path("id") id: Long,
        @Body body: CreateQuestionRequest,
    ): ApiResponse<QuestionDto>

    @DELETE("questions/{id}")
    suspend fun adminDeleteQuestion(@Path("id") id: Long): ApiResponse<Unit>

    @POST("questions/import")
    suspend fun adminImportQuestions(
        @Body body: ImportQuestionsRequest,
    ): ApiResponse<ImportQuestionsResultDto>

    @GET("questions")
    suspend fun listQuestions(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("tag") tag: String? = null,
        @Query("q") keyword: String? = null,
    ): ApiResponse<ListQuestionsDto>

    @GET("questions/{id}")
    suspend fun question(@Path("id") id: Long): ApiResponse<QuestionDto>

    @GET("tags")
    suspend fun tags(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("q") keyword: String? = null,
    ): ApiResponse<TagListDto>

    @POST("questions/{id}/answer-records")
    suspend fun submitAnswer(
        @Path("id") id: Long,
        @Body body: SubmitAnswerRequest,
    ): ApiResponse<Unit>

    @GET("questions/{id}/answer-records")
    suspend fun answerRecordsForQuestion(
        @Path("id") id: Long,
    ): ApiResponse<List<QuestionAnswerRecordDto>>

    @GET("answer-records")
    suspend fun answerRecords(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("trueOrFalse") trueOrFalse: Int = 0,
    ): ApiResponse<ListAnswerRecordsDto>

    @POST("questions/{id}/favorites")
    suspend fun addFavorite(@Path("id") id: Long): ApiResponse<FavoriteDto>

    @GET("favorites")
    suspend fun favorites(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
    ): ApiResponse<ListFavoritesDto>

    @DELETE("favorites/{id}")
    suspend fun deleteFavorite(@Path("id") id: Long): ApiResponse<Unit>

    @POST("questions/{id}/reviews")
    suspend fun addReview(@Path("id") id: Long): ApiResponse<Unit>

    @GET("reviews/today")
    suspend fun todayReviews(): ApiResponse<List<ReviewDto>>

    @GET("reviews")
    suspend fun reviews(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
    ): ApiResponse<ListReviewsDto>

    @POST("reviews/{id}/records")
    suspend fun submitReview(
        @Path("id") id: Long,
        @Body body: ReviewAnswerRequest,
    ): ApiResponse<ReviewDto>

    @DELETE("reviews/{id}")
    suspend fun deleteReview(@Path("id") id: Long): ApiResponse<Unit>
}
