package com.biglv.recitelsland.feature

import com.biglv.recitelsland.data.model.AnswerRecord
import com.biglv.recitelsland.data.model.Question
import com.biglv.recitelsland.data.model.QuestionFilter
import com.biglv.recitelsland.data.model.QuestionType
import com.biglv.recitelsland.feature.bank.BankUiState
import com.biglv.recitelsland.feature.records.RecordsUiState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class UiStateTest {

    private fun question(
        id: Long,
        type: QuestionType,
        tags: List<String> = emptyList(),
    ): Question = Question(
        id = id,
        title = "title-$id",
        context = "context",
        type = type,
        explanation = "explanation",
        tags = tags,
    )

    private val questions = listOf(
        question(1, QuestionType.MultipleChoice, tags = listOf("网络", "TCP")),
        question(2, QuestionType.TrueFalse, tags = listOf("网络")),
        question(3, QuestionType.Reading, tags = listOf("系统设计")),
        question(4, QuestionType.MultipleChoice),
    )

    @Test
    fun bankFilter_countsAndVisibleListStayConsistent() {
        val state = BankUiState(loading = false, questions = questions)

        assertEquals(4, state.countOf(QuestionFilter.All))
        assertEquals(2, state.countOf(QuestionFilter.MultipleChoice))
        assertEquals(1, state.countOf(QuestionFilter.TrueFalse))
        assertEquals(1, state.countOf(QuestionFilter.Reading))

        assertEquals(listOf(2L), state.copy(filter = QuestionFilter.TrueFalse).visibleQuestions.map { it.id })
        assertEquals(listOf(3L), state.copy(filter = QuestionFilter.Reading).visibleQuestions.map { it.id })
    }

    @Test
    fun recordsAccuracyUsesLoadedPage() {
        val records = listOf(
            record(1, correct = true),
            record(2, correct = false),
            record(3, correct = true),
        )
        val state = RecordsUiState(loading = false, records = records)

        assertEquals(2, state.correctCount)
        assertEquals(1, state.incorrectCount)
        assertEquals(66, state.accuracy)
    }

    @Test
    fun emptyRecordsHaveZeroAccuracy() {
        val state = RecordsUiState(loading = false)
        assertEquals(0, state.accuracy)
        assertEquals(0, state.correctCount)
    }

    private fun record(id: Long, correct: Boolean): AnswerRecord = AnswerRecord(
        id = id,
        question = question(id, QuestionType.MultipleChoice),
        answer = listOf("A"),
        correct = correct,
        createdAt = LocalDateTime.of(2026, 11, 1, 10, 0),
    )
}
