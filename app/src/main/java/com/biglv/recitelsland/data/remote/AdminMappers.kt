package com.biglv.recitelsland.data.remote

import com.biglv.recitelsland.data.model.AdminStats
import com.biglv.recitelsland.data.model.AdminUser
import com.biglv.recitelsland.data.model.AdminUserDetail
import com.biglv.recitelsland.data.model.AdminUserPage
import com.biglv.recitelsland.data.model.FailedRequest
import com.biglv.recitelsland.data.model.FailedRequestPage
import com.biglv.recitelsland.data.model.FeedbackItem
import com.biglv.recitelsland.data.model.FeedbackPage
import com.biglv.recitelsland.data.model.ImportQuestionFailure
import com.biglv.recitelsland.data.model.ImportQuestionResult

fun AdminUserDto.toDomain(): AdminUser = AdminUser(
    id = id,
    username = username,
    nickname = nickname,
    email = email,
    avatar = avatar,
    role = role,
    status = status,
    learnedQuestionTotal = allLearnQuestionTotal,
    createdAt = parseServerTime(createdAt),
)

fun AdminUserListDto.toDomain(): AdminUserPage = AdminUserPage(
    items = items.map { it.toDomain() },
    total = total,
    page = page,
    limit = limit,
)

fun AdminUserDetailDto.toDomain(): AdminUserDetail = AdminUserDetail(
    user = AdminUser(
        id = id,
        username = username,
        nickname = nickname,
        email = email,
        avatar = avatar,
        role = role,
        status = status,
        learnedQuestionTotal = allLearnQuestionTotal,
        createdAt = parseServerTime(createdAt),
    ),
    answerRecords = answerRecords,
    favorites = favorites,
    reviews = reviews,
    reviewRecords = reviewRecords,
)

fun AdminStatsDto.toDomain(): AdminStats = AdminStats(
    users = users,
    questions = questions,
    answerRecords = answerRecords,
    reviews = reviews,
    reviewRecords = reviewRecords,
    favorites = favorites,
    feedbacks = feedbacks,
    pendingFeedbacks = pendingFeedbacks,
)

fun FailedRequestDto.toDomain(): FailedRequest = FailedRequest(
    requestId = requestId,
    time = parseServerTime(time),
    method = method,
    path = path,
    status = status,
    code = code,
    message = message,
    ip = ip,
    userId = userId,
    latencyMs = latencyMs,
)

fun FailedRequestListDto.toDomain(): FailedRequestPage = FailedRequestPage(
    items = items.map { it.toDomain() },
    total = total,
    page = page,
    limit = limit,
)

fun FeedbackDto.toDomain(): FeedbackItem = FeedbackItem(
    id = id,
    type = type,
    content = content,
    contact = contact,
    appVersion = appVersion,
    platform = platform,
    status = status,
    adminReply = adminReply,
    createdAt = parseServerTime(createdAt),
    userId = userId,
    userNickname = userNickname,
    userEmail = userEmail,
)

fun FeedbackListDto.toDomain(): FeedbackPage = FeedbackPage(
    items = items.map { it.toDomain() },
    total = total,
    page = page,
    limit = limit,
)

fun ImportQuestionsResultDto.toDomain(): ImportQuestionResult = ImportQuestionResult(
    total = total,
    imported = imported,
    skipped = skipped,
    failed = failed,
    errors = errors.map { ImportQuestionFailure(index = it.index, reason = it.reason) },
)
