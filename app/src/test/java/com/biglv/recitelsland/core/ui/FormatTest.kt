package com.biglv.recitelsland.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class FormatTest {

    private val now = LocalDateTime.of(2026, 11, 1, 10, 0)

    @Test
    fun recordLabelUsesTodayYesterdayAndAbsoluteDate() {
        assertEquals(
            "今天 09:05",
            LocalDateTime.of(2026, 11, 1, 9, 5).toRecordLabel(now),
        )
        assertEquals(
            "昨天 23:10",
            LocalDateTime.of(2026, 10, 31, 23, 10).toRecordLabel(now),
        )
        assertEquals(
            "10月20日 08:30",
            LocalDateTime.of(2026, 10, 20, 8, 30).toRecordLabel(now),
        )
        assertEquals(
            "2025年12月31日 23:00",
            LocalDateTime.of(2025, 12, 31, 23, 0).toRecordLabel(now),
        )
    }

    @Test
    fun dueLabelCoversOverdueTodayTomorrowAndFuture() {
        assertEquals(
            "逾期 2 天",
            LocalDateTime.of(2026, 10, 30, 20, 0).toDueLabel(now),
        )
        assertEquals(
            "今天",
            LocalDateTime.of(2026, 11, 1, 21, 0).toDueLabel(now),
        )
        assertEquals(
            "明天",
            LocalDateTime.of(2026, 11, 2, 9, 0).toDueLabel(now),
        )
        assertEquals(
            "11月5日",
            LocalDateTime.of(2026, 11, 5, 9, 0).toDueLabel(now),
        )
    }
}
