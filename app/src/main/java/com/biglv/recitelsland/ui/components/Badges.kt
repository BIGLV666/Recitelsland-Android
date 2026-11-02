package com.biglv.recitelsland.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.biglv.recitelsland.data.model.QuestionType

@Composable
fun TypeBadge(
    type: QuestionType,
    modifier: Modifier = Modifier,
) {
    val (container, content) = when (type) {
        QuestionType.MultipleChoice -> MaterialTheme.colorScheme.primaryContainer to
            MaterialTheme.colorScheme.onPrimaryContainer
        QuestionType.TrueFalse -> MaterialTheme.colorScheme.secondaryContainer to
            MaterialTheme.colorScheme.onSecondaryContainer
        QuestionType.Reading -> MaterialTheme.colorScheme.tertiaryContainer to
            MaterialTheme.colorScheme.onTertiaryContainer
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = container,
        contentColor = content,
    ) {
        Text(
            text = type.label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun TagText(
    tag: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "#$tag",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
fun ResultBadge(
    correct: Boolean,
    modifier: Modifier = Modifier,
) {
    val container: Color
    val content: Color
    val text: String
    if (correct) {
        container = MaterialTheme.colorScheme.primaryContainer
        content = MaterialTheme.colorScheme.onPrimaryContainer
        text = "答对"
    } else {
        container = MaterialTheme.colorScheme.errorContainer
        content = MaterialTheme.colorScheme.onErrorContainer
        text = "答错"
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = container,
        contentColor = content,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
