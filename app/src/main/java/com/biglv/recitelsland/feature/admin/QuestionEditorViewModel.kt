package com.biglv.recitelsland.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biglv.recitelsland.data.model.QuestionType
import com.biglv.recitelsland.data.remote.CreateQuestionRequest
import com.biglv.recitelsland.data.repo.AdminRepository
import com.biglv.recitelsland.data.repo.ReciteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuestionEditorUiState(
    val loading: Boolean = false,
    val editing: Boolean = false,
    val type: QuestionType = QuestionType.MultipleChoice,
    val title: String = "",
    val context: String = "",
    val optionsText: String = "",
    val answerText: String = "",
    val explanation: String = "",
    val tagsText: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val message: String? = null,
)

class QuestionEditorViewModel(
    private val repository: ReciteRepository,
    private val adminRepository: AdminRepository,
    private val questionId: Long?,
) : ViewModel() {

    private val _state = MutableStateFlow(QuestionEditorUiState(editing = questionId != null))
    val state: StateFlow<QuestionEditorUiState> = _state.asStateFlow()

    init {
        if (questionId != null) load(questionId)
    }

    private fun load(id: Long) {
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            runCatching { repository.question(id) }
                .onSuccess { question ->
                    if (question == null) {
                        _state.update { it.copy(loading = false, message = "题目不存在") }
                    } else {
                        _state.update {
                            it.copy(
                                loading = false,
                                editing = true,
                                type = question.type,
                                title = question.title,
                                context = question.context,
                                optionsText = question.options.joinToString("\n"),
                                answerText = question.answer.joinToString(","),
                                explanation = question.explanation,
                                tagsText = question.tags.joinToString(","),
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, message = error.message ?: "题目加载失败") }
                }
        }
    }

    fun setTitle(value: String) = _state.update { it.copy(title = value) }
    fun setContext(value: String) = _state.update { it.copy(context = value) }
    fun setOptions(value: String) = _state.update { it.copy(optionsText = value) }
    fun setAnswer(value: String) = _state.update { it.copy(answerText = value) }
    fun setExplanation(value: String) = _state.update { it.copy(explanation = value) }
    fun setTags(value: String) = _state.update { it.copy(tagsText = value) }

    fun setType(type: QuestionType) {
        if (_state.value.editing) return
        _state.update {
            it.copy(
                type = type,
                optionsText = if (type == QuestionType.TrueFalse && it.optionsText.isBlank()) {
                    "正确\n错误"
                } else {
                    it.optionsText
                },
            )
        }
    }

    fun save() {
        val current = _state.value
        if (current.saving) return
        if (current.title.isBlank() || current.context.isBlank() || current.explanation.isBlank()) {
            _state.update { it.copy(message = "标题、题干和解析不能为空") }
            return
        }

        val options = current.optionsText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val answers = current.answerText
            .split(Regex("[,，、\\s]+"))
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() }
        val tags = current.tagsText
            .split(Regex("[,，、]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (current.type == QuestionType.MultipleChoice && (options.size < 2 || answers.isEmpty())) {
            _state.update { it.copy(message = "选择题至少需要两个选项和一个答案") }
            return
        }
        if (current.type == QuestionType.TrueFalse &&
            (options.size < 2 || answers.size != 1 || answers[0] !in listOf("A", "B"))
        ) {
            _state.update { it.copy(message = "判断题需要两个选项，答案为 A 或 B") }
            return
        }

        val request = CreateQuestionRequest(
            questionTitle = current.title.trim(),
            questionContext = current.context.trim(),
            questionType = current.type.apiValue,
            answer = if (current.type == QuestionType.Reading) emptyList() else answers,
            options = if (current.type == QuestionType.Reading) emptyList() else options,
            explanation = current.explanation.trim(),
            tags = tags,
        )

        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            runCatching {
                if (questionId == null) {
                    adminRepository.createQuestion(request)
                } else {
                    adminRepository.updateQuestion(questionId, request)
                }
            }.onSuccess {
                _state.update {
                    it.copy(saving = false, saved = true, message = if (questionId == null) "题目已创建" else "题目已更新")
                }
            }.onFailure { error ->
                _state.update { it.copy(saving = false, message = error.message ?: "保存失败") }
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}
