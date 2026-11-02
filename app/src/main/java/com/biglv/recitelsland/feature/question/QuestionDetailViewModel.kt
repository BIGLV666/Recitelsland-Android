package com.biglv.recitelsland.feature.question

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biglv.recitelsland.data.model.AnswerEvaluator
import com.biglv.recitelsland.data.model.AnswerRecord
import com.biglv.recitelsland.data.model.Question
import com.biglv.recitelsland.data.model.QuestionType
import com.biglv.recitelsland.data.repo.ReciteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

enum class DetailTab(val label: String) {
    Question("题目"),
    Records("记录"),
}

data class AnswerCardState(
    val selected: List<String> = emptyList(),
    val submitting: Boolean = false,
    val submitted: Boolean = false,
    val correct: Boolean? = null,
    val correctAnswer: List<String> = emptyList(),
    val learning: Boolean = false,
    val learned: Boolean = false,
)

data class QuestionDetailUiState(
    val loading: Boolean = true,
    val question: Question? = null,
    val tab: DetailTab = DetailTab.Question,
    val card: AnswerCardState = AnswerCardState(),
    val records: List<AnswerRecord> = emptyList(),
    val recordsLoading: Boolean = false,
    val recordsLoaded: Boolean = false,
    val favoriteId: Long? = null,
    val inReview: Boolean = false,
    val message: String? = null,
)

class QuestionDetailViewModel(
    private val repository: ReciteRepository,
    private val questionId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(QuestionDetailUiState())
    val state: StateFlow<QuestionDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching { repository.question(questionId) }
                .onSuccess { question ->
                    if (question == null) {
                        _state.update { it.copy(loading = false, message = "题目不存在") }
                    } else {
                        _state.update { it.copy(loading = false, question = question) }
                        loadRecords()
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(loading = false, message = error.message ?: "题目加载失败")
                    }
                }
        }
    }

    fun selectTab(tab: DetailTab) {
        _state.update { it.copy(tab = tab) }
        if (tab == DetailTab.Records && !_state.value.recordsLoaded) {
            loadRecords()
        }
    }

    fun loadRecords() {
        if (_state.value.recordsLoading) return
        _state.update { it.copy(recordsLoading = true) }
        viewModelScope.launch {
            runCatching { repository.answerRecordsForQuestion(questionId) }
                .onSuccess { records ->
                    _state.update {
                        it.copy(
                            records = records,
                            recordsLoading = false,
                            recordsLoaded = true,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            recordsLoading = false,
                            message = error.message ?: "作答记录加载失败",
                        )
                    }
                }
        }
    }

    fun toggleOption(letter: String) {
        val current = _state.value
        val question = current.question ?: return
        if (current.card.submitted || current.card.submitting) return

        val selected = AnswerEvaluator.toggleSelection(
            type = question.type,
            current = current.card.selected,
            letter = letter,
        )
        updateCard(current.card.copy(selected = selected))
    }

    fun submit() {
        val current = _state.value
        val question = current.question ?: return
        val card = current.card
        if (card.selected.isEmpty() || card.submitting || card.submitted) return

        updateCard(card.copy(submitting = true))
        viewModelScope.launch {
            runCatching { repository.submitAnswer(question.id, card.selected) }
                .onSuccess {
                    val correct = AnswerEvaluator.isCorrect(
                        type = question.type,
                        selected = card.selected,
                        correctAnswer = question.answer,
                    )
                    val record = AnswerRecord(
                        id = -System.currentTimeMillis(),
                        question = question,
                        answer = card.selected,
                        correct = correct,
                        createdAt = LocalDateTime.now(),
                    )
                    _state.update {
                        it.copy(
                            card = it.card.copy(
                                submitting = false,
                                submitted = true,
                                correct = correct,
                                correctAnswer = question.answer,
                            ),
                            records = listOf(record) + it.records,
                            recordsLoaded = true,
                            message = if (correct) "回答正确" else "回答错误，看看解析",
                        )
                    }
                }
                .onFailure { error ->
                    updateCard(card.copy(submitting = false))
                    _state.update { it.copy(message = error.message ?: "提交失败，请重试") }
                }
        }
    }

    fun retryAnswer() {
        val card = _state.value.card
        updateCard(
            card.copy(
                selected = emptyList(),
                submitted = false,
                correct = null,
                correctAnswer = emptyList(),
            ),
        )
    }

    fun completeReading() {
        val current = _state.value
        val question = current.question ?: return
        val card = current.card
        if (card.learning || card.learned) return

        updateCard(card.copy(learning = true))
        viewModelScope.launch {
            runCatching { repository.completeReading(question.id) }
                .onSuccess {
                    updateCard(card.copy(learning = false, learned = true))
                    _state.update { it.copy(message = "完成学习，学习次数 +1") }
                }
                .onFailure { error ->
                    updateCard(card.copy(learning = false))
                    _state.update { it.copy(message = error.message ?: "记录学习次数失败") }
                }
        }
    }

    fun toggleFavorite() {
        val current = _state.value
        val question = current.question ?: return
        val favoriteId = current.favoriteId
        if (favoriteId != null) {
            viewModelScope.launch {
                runCatching { repository.deleteFavorite(favoriteId) }
                    .onSuccess {
                        _state.update { it.copy(favoriteId = null, message = "已取消收藏") }
                    }
                    .onFailure { error ->
                        _state.update { it.copy(message = error.message ?: "取消收藏失败") }
                    }
            }
        } else {
            viewModelScope.launch {
                runCatching { repository.addFavorite(question.id) }
                    .onSuccess { id ->
                        _state.update { it.copy(favoriteId = id, message = "已加入收藏") }
                    }
                    .onFailure { error ->
                        _state.update { it.copy(message = error.message ?: "收藏失败") }
                    }
            }
        }
    }

    fun addReview() {
        val current = _state.value
        val question = current.question ?: return
        if (current.inReview) return
        viewModelScope.launch {
            runCatching { repository.addReview(question.id) }
                .onSuccess {
                    _state.update { it.copy(inReview = true, message = "已加入复习计划") }
                }
                .onFailure { error ->
                    _state.update { it.copy(message = error.message ?: "加入复习失败") }
                }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun updateCard(card: AnswerCardState) {
        _state.update { it.copy(card = card) }
    }
}
