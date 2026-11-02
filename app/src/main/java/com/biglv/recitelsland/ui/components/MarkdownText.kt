package com.biglv.recitelsland.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.layout.fillMaxWidth
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography

/**
 * 统一的 Markdown 渲染入口。
 *
 * 题干、阅读材料、解析都走这里；后续要换渲染库时只改这一处。
 */
@Composable
fun MarkdownText(
    content: String,
    modifier: Modifier = Modifier,
) {
    if (content.isBlank()) return

    Markdown(
        content = content,
        modifier = modifier.fillMaxWidth(),
        colors = markdownColor(
            text = MaterialTheme.colorScheme.onSurface,
            codeBackground = MaterialTheme.colorScheme.surfaceVariant,
            inlineCodeBackground = MaterialTheme.colorScheme.surfaceVariant,
            dividerColor = MaterialTheme.colorScheme.outlineVariant,
            tableBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
        typography = markdownTypography(
            h1 = MaterialTheme.typography.headlineSmall,
            h2 = MaterialTheme.typography.titleLarge,
            h3 = MaterialTheme.typography.titleMedium,
            h4 = MaterialTheme.typography.titleSmall,
            h5 = MaterialTheme.typography.titleSmall,
            h6 = MaterialTheme.typography.titleSmall,
            text = MaterialTheme.typography.bodyLarge,
            paragraph = MaterialTheme.typography.bodyLarge,
            ordered = MaterialTheme.typography.bodyLarge,
            bullet = MaterialTheme.typography.bodyLarge,
            list = MaterialTheme.typography.bodyLarge,
            quote = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            code = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            inlineCode = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            link = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.primary,
                fontStyle = FontStyle.Normal,
            ),
        ),
    )
}
