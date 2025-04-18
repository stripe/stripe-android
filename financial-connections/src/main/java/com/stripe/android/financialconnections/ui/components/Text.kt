package com.stripe.android.financialconnections.ui.components

import android.graphics.Typeface
import android.text.Annotation
import android.text.SpannedString
import android.text.style.StyleSpan
import android.text.style.URLSpan
import androidx.annotation.StringRes
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
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
        StringAnnotation.CLICKABLE to defaultStyle
            .toSpanStyle()
            .copy(textDecoration = TextDecoration.Underline)
    ),
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val pressedColor = FinancialConnectionsTheme.colors.textDefault
    var pressedAnnotation: String? by remember { mutableStateOf(null) }
    val resource = annotatedStringResource(
        resource = text,
        spanStyleForAnnotation = { annotation ->
            val matchingAnnotation = StringAnnotation.entries
                .firstOrNull { it.value == annotation.key }
            val spanStyle = annotationStyles[matchingAnnotation]
            if (pressedAnnotation == annotation.value) {
                spanStyle?.copy(color = pressedColor)
            } else {
                spanStyle
            }
        }
    )
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val pressIndicator = Modifier.pointerInput(onClickableTextClick) {
        detectTapGestures(
            onPress = { offset ->
                val clickedAnnotation = layoutResult?.clickedAnnotation(offset, resource)
                // mark the current clickable text portion as pressed
                pressedAnnotation = clickedAnnotation?.item
                // release the current pressed text portion.
                tryAwaitRelease()
                pressedAnnotation = null
            },
            onTap = { offset ->
                layoutResult?.clickedAnnotation(offset, resource)?.let {
                    onClickableTextClick(it.item)
                }
            }
        )
    }
    BasicText(
        text = resource,
        modifier = modifier.then(pressIndicator),
        style = defaultStyle,
        softWrap = true,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = {
            layoutResult = it
        }
    )
}

private fun TextLayoutResult.clickedAnnotation(
    offset: Offset,
    resource: AnnotatedString,
): AnnotatedString.Range<String>? = getOffsetForPosition(offset).let {
    resource.getStringAnnotations(
        tag = StringAnnotation.CLICKABLE.value,
        start = it,
        end = it
    ).firstOrNull()
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
    spanStyleForAnnotation: (Annotation) -> SpanStyle? = { null }
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
                spanStyleForAnnotation(it)?.let { style ->
                    resultBuilder.addStyle(style, spanStart, spanEnd)
                }
            }
        }
    return resultBuilder.toAnnotatedString()
}

@Composable
internal fun pluralStringResource(
    @StringRes singular: Int,
    @StringRes plural: Int,
    count: Int,
    vararg formatArgs: Any
): String {
    val quantityString = if (count == 1) {
        stringResource(singular, *formatArgs)
    } else {
        stringResource(plural, *formatArgs)
    }
    return quantityString
}

internal enum class StringAnnotation(val value: String) {
    CLICKABLE("clickable"),
    BOLD("bold")
}
