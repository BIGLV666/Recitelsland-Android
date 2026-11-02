package com.biglv.recitelsland.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerEvaluatorTest {

    @Test
    fun multipleChoice_orderDoesNotMatter() {
        assertTrue(
            AnswerEvaluator.isCorrect(
                QuestionType.MultipleChoice,
                selected = listOf("B", "A", "D"),
                correctAnswer = listOf("A", "B", "D"),
            ),
        )
    }

    @Test
    fun multipleChoice_missingOrExtraOptionIsWrong() {
        assertFalse(
            AnswerEvaluator.isCorrect(
                QuestionType.MultipleChoice,
                selected = listOf("A", "B"),
                correctAnswer = listOf("A", "B", "D"),
            ),
        )
        assertFalse(
            AnswerEvaluator.isCorrect(
                QuestionType.MultipleChoice,
                selected = listOf("A", "B", "D", "C"),
                correctAnswer = listOf("A", "B", "D"),
            ),
        )
    }

    @Test
    fun trueFalse_requiresExactlyOneMatchingAnswer() {
        assertTrue(
            AnswerEvaluator.isCorrect(
                QuestionType.TrueFalse,
                selected = listOf("A"),
                correctAnswer = listOf("A"),
            ),
        )
        assertFalse(
            AnswerEvaluator.isCorrect(
                QuestionType.TrueFalse,
                selected = listOf("A", "B"),
                correctAnswer = listOf("A"),
            ),
        )
        assertFalse(
            AnswerEvaluator.isCorrect(
                QuestionType.TrueFalse,
                selected = emptyList(),
                correctAnswer = listOf("A"),
            ),
        )
    }

    @Test
    fun multipleChoice_toggleAddsAndRemovesWithoutDuplicates() {
        val first = AnswerEvaluator.toggleSelection(
            QuestionType.MultipleChoice,
            current = listOf("A"),
            letter = "B",
        )
        assertEquals(listOf("A", "B"), first)

        val second = AnswerEvaluator.toggleSelection(
            QuestionType.MultipleChoice,
            current = first,
            letter = "A",
        )
        assertEquals(listOf("B"), second)
    }

    @Test
    fun trueFalse_toggleReplacesSelection() {
        val selected = AnswerEvaluator.toggleSelection(
            QuestionType.TrueFalse,
            current = listOf("A"),
            letter = "B",
        )
        assertEquals(listOf("B"), selected)
    }
}
