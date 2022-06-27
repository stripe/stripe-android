@file:Suppress("MatchingDeclarationName")

package com.stripe.android.financialconnections.ui.components

import android.text.Annotation
import android.text.SpannedString
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.core.text.getSpans
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun AnnotatedText(
    resource: TextResource,
    onClickableTextClick: (String) -> Unit,
    textStyle: TextStyle
) {
    val urlStyle = FinancialConnectionsTheme.typography.bodyEmphasized
        .toSpanStyle()
        .copy(color = FinancialConnectionsTheme.colors.textBrand)
    val resource = annotatedStringResource(
        resource = resource
    ) { annotation ->
        when (StringAnnotation.values().firstOrNull { it.value == annotation.key }) {
            StringAnnotation.CLICKABLE -> urlStyle
            else -> null
        }
    }
    ClickableText(
        text = resource,
        style = textStyle,
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

@Composable
private fun annotatedStringResource(
    resource: TextResource,
    spanStyles: (Annotation) -> SpanStyle? = { null }
): AnnotatedString {
    val spannedString = SpannedString(resource.toText())
    val resultBuilder = AnnotatedString.Builder()
    resultBuilder.append(spannedString.toString())
    spannedString.getSpans<Annotation>(0, spannedString.length).forEach { annotation ->
        val spanStart = spannedString.getSpanStart(annotation)
        val spanEnd = spannedString.getSpanEnd(annotation)
        resultBuilder.addStringAnnotation(
            tag = annotation.key,
            annotation = annotation.value,
            start = spanStart,
            end = spanEnd
        )
        spanStyles(annotation)?.let { resultBuilder.addStyle(it, spanStart, spanEnd) }
    }
    return resultBuilder.toAnnotatedString()
}

internal enum class StringAnnotation(val value: String) {
    CLICKABLE("clickable")
}
