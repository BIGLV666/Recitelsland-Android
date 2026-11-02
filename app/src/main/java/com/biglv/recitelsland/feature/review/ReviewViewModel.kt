package com.biglv.recitelsland.feature.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biglv.recitelsland.data.model.AnswerEvaluator
import com.biglv.recitelsland.data.model.QuestionType
import com.biglv.recitelsland.data.model.ReviewItem
import com.biglv.recitelsland.data.repo.ReciteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReviewUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val items: List<ReviewItem> = emptyList(),
    val selected: ReviewItem? = null,
    val selectedAnswers: List<String> = emptyList(),
    val rating: Int = -1,
    val submitting: Boolean = false,
    val formError: String? = null,
    val message: String? = null,
) {
    val overdueCount: Int get() = items.count { it.isOverdue }
}

class ReviewViewModel(
    private val repository: ReciteRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewUiState())
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun refresh() {
        if (_state.value.loading || _state.value.refreshing) return
        load(refreshing = true)
    }

    fun load(refreshing: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(loading = !refreshing, refreshing = refreshing) }
            runCatching { repository.todayReviews() }
                .onSuccess { items ->
                    _state.update {
                        it.copy(loading = false, refreshing = false, items = items)
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            message = error.message ?: "复习计划加载失败",
                        )
                    }
                }
        }
    }

    fun openReview(item: ReviewItem) {
        _state.update {
            it.copy(
                selected = item,
                selectedAnswers = emptyList(),
                rating = if (item.question.type == QuestionType.Reading) 3 else -1,
                formError = null,
                message = null,
            )
        }
    }

    fun closeSheet() {
        _state.update { it.copy(selected = null, selectedAnswers = emptyList(), rating = -1, formError = null) }
    }

    fun toggleAnswer(letter: String) {
        val current = _state.value
        val item = current.selected ?: return
        if (current.submitting) return
        val selected = AnswerEvaluator.toggleSelection(
            type = item.question.type,
            current = current.selectedAnswers,
            letter = letter,
        )
        _state.update { it.copy(selectedAnswers = selected, formError = null) }
    }

    fun setRating(value: Int) {
        _state.update { it.copy(rating = value.coerceIn(0, 5), formError = null) }
    }

    fun submit() {
        val current = _state.value
        val item = current.selected ?: return
        if (current.submitting) return

        val reading = item.question.type == QuestionType.Reading
        if (reading && current.rating < 0) {
            _state.update { it.copy(formError = "请选择这篇材料对你的难度") }
            return
        }
        if (!reading && current.selectedAnswers.isEmpty()) {
            _state.update { it.copy(formError = "请先选择答案") }
            return
        }

        _state.update { it.copy(submitting = true, formError = null, message = null) }
        viewModelScope.launch {
            runCatching {
                repository.submitReview(
                    reviewId = item.reviewId,
                    q = if (reading) current.rating else 0,
                    answers = if (reading) emptyList() else current.selectedAnswers,
                )
            }.onSuccess {
                val correct = if (reading) {
                    true
                } else {
                    AnswerEvaluator.isCorrect(
                        type = item.question.type,
                        selected = current.selectedAnswers,
                        correctAnswer = item.question.answer,
                    )
                }
                _state.update {
                    it.copy(
                        submitting = false,
                        selected = null,
                        selectedAnswers = emptyList(),
                        rating = -1,
                        items = it.items.filterNot { review -> review.reviewId == item.reviewId },
                        message = if (reading) {
                            "复习已记录"
                        } else if (correct) {
                            "回答正确，复习已记录"
                        } else {
                            "回答错误，复习已记录"
                        },
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(submitting = false, formError = error.message ?: "复习提交失败")
                }
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }
}
