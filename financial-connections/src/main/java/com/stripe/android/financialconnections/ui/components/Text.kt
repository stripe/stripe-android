package com.stripe.android.financialconnections.ui.components

import android.text.SpannedString
import androidx.annotation.StringRes
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.core.text.getSpans
import com.stripe.android.financialconnections.ui.clickableTextSpanStyle

enum class StringAnnotation(val value: String) {
    CLICKABLE("clickable")
}

@Composable
fun AnnotatedText(
    annotatedTextResourceId: Int,
    onClickableTextClick: (String) -> Unit,
    textStyle: TextStyle
) {
    val urlStyle = clickableTextSpanStyle()
    val consentTc = annotatedStringResource(
        id = annotatedTextResourceId,
        spanStyles = { annotation ->
            when (StringAnnotation.values().firstOrNull { it.value == annotation.key }) {
                StringAnnotation.CLICKABLE -> urlStyle
                else -> null
            }
        }
    )
    ClickableText(
        text = consentTc,
        style = textStyle,
        onClick = { offset ->
            consentTc.getStringAnnotations(
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
    @StringRes id: Int,
    spanStyles: (android.text.Annotation) -> SpanStyle? = { null }
): AnnotatedString {
    val resources = LocalContext.current.resources
    val spannedString = SpannedString(resources.getText(id))
    val resultBuilder = AnnotatedString.Builder()
    resultBuilder.append(spannedString.toString())
    spannedString.getSpans<android.text.Annotation>(0, spannedString.length).forEach { annotation ->
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
