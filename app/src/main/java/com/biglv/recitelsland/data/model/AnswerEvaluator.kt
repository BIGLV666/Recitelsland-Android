package com.biglv.recitelsland.data.model

/**
 * 作答判定规则与后端保持一致：
 * - 选择题：答案集合相等，顺序不影响结果
 * - 判断题：只有一个答案，必须完全一致
 * - 阅读题：没有标准作答
 */
object AnswerEvaluator {
    fun isCorrect(
        type: QuestionType,
        selected: List<String>,
        correctAnswer: List<String>,
    ): Boolean = when (type) {
        QuestionType.MultipleChoice -> selected.toSet() == correctAnswer.toSet()
        QuestionType.TrueFalse -> selected.singleOrNull() == correctAnswer.singleOrNull()
        QuestionType.Reading -> true
    }

    fun toggleSelection(type: QuestionType, current: List<String>, letter: String): List<String> =
        if (type == QuestionType.TrueFalse) {
            listOf(letter)
        } else if (letter in current) {
            current - letter
        } else {
            current + letter
        }
}
