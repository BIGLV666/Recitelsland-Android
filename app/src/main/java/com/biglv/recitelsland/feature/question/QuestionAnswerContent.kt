package com.biglv.recitelsland.feature.question

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.biglv.recitelsland.data.model.Question
import com.biglv.recitelsland.data.model.QuestionType
import com.biglv.recitelsland.ui.components.MarkdownText
import com.biglv.recitelsland.ui.components.TagText
import com.biglv.recitelsland.ui.components.TypeBadge
import com.biglv.recitelsland.ui.theme.LocalSemanticColors

/**
 * 题目详情正文：题干、选项、作答反馈、解析和操作区。
 * 列表页不再复用这块内容，避免长文本在题库里展开。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuestionAnswerContent(
    question: Question,
    card: AnswerCardState,
    favorite: Boolean,
    inReview: Boolean,
    onToggleOption: (String) -> Unit,
    onSubmit: () -> Unit,
    onCompleteReading: () -> Unit,
    onRetryAnswer: () -> Unit,
    onFavorite: () -> Unit,
    onAddReview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = LocalSemanticColors.current

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TypeBadge(type = question.type)
            if (question.tags.isNotEmpty()) {
                Spacer(Modifier.width(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    question.tags.forEach { TagText(tag = it) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = question.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(14.dp))
        MarkdownText(content = question.context)

        if (question.type != QuestionType.Reading) {
            Spacer(Modifier.height(20.dp))
            SectionLabel("选项")
            question.options.forEachIndexed { index, option ->
                val letter = ('A' + index).toString()
                OptionRow(
                    questionId = question.id,
                    letter = letter,
                    text = option,
                    selected = letter in card.selected,
                    submitted = card.submitted,
                    correct = card.submitted && letter in card.correctAnswer,
                    wrong = card.submitted && letter in card.selected && letter !in card.correctAnswer,
                    onClick = { onToggleOption(letter) },
                )
                if (index != question.options.lastIndex) Spacer(Modifier.height(9.dp))
            }

            Spacer(Modifier.height(18.dp))
            if (!card.submitted) {
                Button(
                    onClick = onSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("submit_${question.id}"),
                    enabled = card.selected.isNotEmpty() && !card.submitting,
                ) {
                    if (card.submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("提交中")
                    } else {
                        Text("提交答案")
                    }
                }
            } else {
                AnswerResult(
                    questionId = question.id,
                    correct = card.correct == true,
                    correctAnswer = card.correctAnswer,
                )
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(16.dp))
                SectionLabel("解析")
                MarkdownText(
                    content = question.explanation,
                    modifier = Modifier.testTag("explanation_${question.id}"),
                )
                TextButton(
                    onClick = onRetryAnswer,
                    modifier = Modifier.testTag("retry_${question.id}"),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("再答一次")
                }
            }
        } else {
            Spacer(Modifier.height(20.dp))
            if (!card.learned) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "完成学习后显示答案与解析，并累加一次学习次数",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onCompleteReading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("complete_reading_${question.id}"),
                    enabled = !card.learning,
                ) {
                    if (card.learning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("记录中")
                    } else {
                        Icon(Icons.Outlined.School, contentDescription = null, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("完成学习")
                    }
                }
            } else {
                Surface(
                    color = semantic.correctContainer,
                    contentColor = semantic.onCorrectContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("已完成学习", style = MaterialTheme.typography.titleSmall)
                            Text("本次学习次数 +1", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                SectionLabel("答案与解析")
                MarkdownText(
                    content = question.explanation,
                    modifier = Modifier.testTag("explanation_${question.id}"),
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onFavorite,
                modifier = Modifier
                    .weight(1f)
                    .testTag("favorite_${question.id}"),
                enabled = !favorite,
            ) {
                Icon(
                    imageVector = if (favorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(if (favorite) "已收藏" else "收藏", maxLines = 1)
            }
            OutlinedButton(
                onClick = onAddReview,
                modifier = Modifier
                    .weight(1f)
                    .testTag("add_review_${question.id}"),
                enabled = !inReview,
            ) {
                Icon(
                    imageVector = if (inReview) Icons.Filled.Check else Icons.AutoMirrored.Outlined.PlaylistAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(if (inReview) "已加入复习" else "加入复习", maxLines = 1)
            }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun OptionRow(
    questionId: Long,
    letter: String,
    text: String,
    selected: Boolean,
    submitted: Boolean,
    correct: Boolean,
    wrong: Boolean,
    onClick: () -> Unit,
) {
    val semantic = LocalSemanticColors.current
    val container: Color = when {
        correct -> semantic.correctContainer
        wrong -> semantic.incorrectContainer
        selected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val content: Color = when {
        correct -> semantic.onCorrectContainer
        wrong -> semantic.onIncorrectContainer
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val border: Color = when {
        correct -> semantic.correct
        wrong -> semantic.incorrect
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("option_${questionId}_$letter")
            .clickable(enabled = !submitted, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content,
        border = BorderStroke(if (selected || correct || wrong) 1.5.dp else 1.dp, border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = if (selected || correct || wrong) border else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (selected || correct || wrong) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(28.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = letter, style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            if (correct) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "正确选项",
                    tint = semantic.correct,
                    modifier = Modifier.size(20.dp),
                )
            } else if (wrong) {
                Icon(
                    imageVector = Icons.Filled.Cancel,
                    contentDescription = "错误选择",
                    tint = semantic.incorrect,
                    modifier = Modifier.size(20.dp),
                )
            } else if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun AnswerResult(
    questionId: Long,
    correct: Boolean,
    correctAnswer: List<String>,
) {
    val semantic = LocalSemanticColors.current
    val container = if (correct) semantic.correctContainer else semantic.incorrectContainer
    val content = if (correct) semantic.onCorrectContainer else semantic.onIncorrectContainer
    val label = if (correct) "回答正确" else "回答错误"
    val answerText = if (correctAnswer.isEmpty()) "" else "正确答案：${correctAnswer.joinToString("、")}"

    Surface(
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("result_$questionId"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (correct) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.titleSmall)
                if (answerText.isNotEmpty()) {
                    Text(text = answerText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
