@file:Suppress("MatchingDeclarationName")

package com.stripe.android.financialconnections.ui.components

import android.graphics.Typeface
import android.text.Annotation
import android.text.SpannedString
import android.text.style.StyleSpan
import android.text.style.URLSpan
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.core.text.getSpans
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun AnnotatedText(
    text: TextResource,
    onClickableTextClick: (String) -> Unit,
    defaultStyle: TextStyle,
    modifier: Modifier = Modifier,
    annotationStyles: Map<StringAnnotation, SpanStyle> = mapOf(
        StringAnnotation.CLICKABLE to FinancialConnectionsTheme.typography.bodyEmphasized
            .toSpanStyle()
            .copy(color = FinancialConnectionsTheme.colors.textBrand)
    )
) {
    val resource = annotatedStringResource(
        resource = text
    ) { annotation ->
        annotationStyles[
            StringAnnotation.values()
                .firstOrNull { it.value == annotation.key }
        ]
    }
    ClickableText(
        text = resource,
        style = defaultStyle,
        modifier = modifier,
        onClick = { offset ->
            resource.getStringAnnotations(
                tag = StringAnnotation.CLICKABLE.value,
                start = offset,
                end = offset
            )
                .firstOrNull()
                ?.let { onClickableTextClick(it.item) }
        }
    )
}

private fun Any.toAnnotation(): Annotation? = when (this) {
    is StyleSpan -> when (this.style) {
        Typeface.BOLD -> Annotation(StringAnnotation.BOLD.value, "")
        Typeface.NORMAL,
        Typeface.ITALIC,
        Typeface.BOLD_ITALIC -> null

        else -> null
    }

    is URLSpan -> Annotation(StringAnnotation.CLICKABLE.value, url)
    is Annotation -> this
    else -> null
}

@Composable
private fun annotatedStringResource(
    resource: TextResource,
    spanStyles: (Annotation) -> SpanStyle? = { null }
): AnnotatedString {
    val spannedString = SpannedString(resource.toText())
    val resultBuilder = AnnotatedString.Builder()
    resultBuilder.append(spannedString.toString())
    spannedString
        .getSpans<Any>(0, spannedString.length)
        .forEach { annotation ->
            val spanStart = spannedString.getSpanStart(annotation)
            val spanEnd = spannedString.getSpanEnd(annotation)
            annotation.toAnnotation()?.let {
                resultBuilder.addStringAnnotation(
                    tag = it.key,
                    annotation = it.value,
                    start = spanStart,
                    end = spanEnd
                )
                spanStyles(it)?.let { resultBuilder.addStyle(it, spanStart, spanEnd) }
            }
        }
    return resultBuilder.toAnnotatedString()
}

internal enum class StringAnnotation(val value: String) {
    CLICKABLE("clickable"),
    BOLD("bold")
}
