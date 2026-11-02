package com.biglv.recitelsland.core.ui

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateTimeFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")
private val fullFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm")

fun LocalDateTime.toRecordLabel(now: LocalDateTime = LocalDateTime.now()): String {
    val date = toLocalDate()
    return when {
        date == now.toLocalDate() -> "今天 ${format(timeFormatter)}"
        date == now.toLocalDate().minusDays(1) -> "昨天 ${format(timeFormatter)}"
        date.year == now.year -> format(dateTimeFormatter)
        else -> format(fullFormatter)
    }
}

fun LocalDateTime.toDueLabel(now: LocalDateTime = LocalDateTime.now()): String {
    val target = toLocalDate()
    val today = now.toLocalDate()
    return when {
        target.isBefore(today) -> "逾期 ${today.toEpochDay() - target.toEpochDay()} 天"
        target == today -> "今天"
        target == today.plusDays(1) -> "明天"
        else -> target.format(DateTimeFormatter.ofPattern("M月d日"))
    }
}

fun LocalDate.toShortLabel(): String = format(DateTimeFormatter.ofPattern("M月d日"))
