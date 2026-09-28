package com.stripe.android.financialconnections.ui.components

import android.graphics.Typeface
import android.text.Annotation
import android.text.SpannedString
import android.text.style.StyleSpan
import android.text.style.URLSpan
import androidx.annotation.StringRes
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
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
    val resource = annotatedStringResource(
        resource = text,
        onClickableTextClick = onClickableTextClick,
        annotationStyles = annotationStyles,
        pressedLinkColor = FinancialConnectionsTheme.colors.textDefault,
    )
    BasicText(
        text = resource,
        modifier = modifier,
        style = defaultStyle,
        softWrap = true,
        overflow = overflow,
        maxLines = maxLines,
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
    onClickableTextClick: (String) -> Unit,
    annotationStyles: Map<StringAnnotation, SpanStyle>,
    pressedLinkColor: Color,
): AnnotatedString = buildAnnotatedStringResource(
    resource = resource.toText(),
    onClickableTextClick = onClickableTextClick,
    annotationStyles = annotationStyles,
    pressedLinkColor = pressedLinkColor,
)

internal fun buildAnnotatedStringResource(
    resource: CharSequence,
    onClickableTextClick: (String) -> Unit,
    annotationStyles: Map<StringAnnotation, SpanStyle>,
    pressedLinkColor: Color,
): AnnotatedString {
    val spannedString = SpannedString(resource)
    val resultBuilder = AnnotatedString.Builder(spannedString.toString())
    spannedString
        .getSpans<Any>(0, spannedString.length)
        .forEach { annotation ->
            val spanStart = spannedString.getSpanStart(annotation)
            val spanEnd = spannedString.getSpanEnd(annotation)
            annotation.toAnnotation()?.let {
                val matchingAnnotation = it.matchingStringAnnotation()
                val spanStyle = annotationStyles[matchingAnnotation]
                when (matchingAnnotation) {
                    StringAnnotation.CLICKABLE -> {
                        resultBuilder.addLink(
                            url = it.toLinkAnnotation(
                                onClickableTextClick = onClickableTextClick,
                                style = spanStyle,
                                pressedLinkColor = pressedLinkColor,
                            ),
                            start = spanStart,
                            end = spanEnd
                        )
                    }

                    null,
                    StringAnnotation.BOLD -> {
                        resultBuilder.addStringAnnotation(
                            tag = it.key,
                            annotation = it.value,
                            start = spanStart,
                            end = spanEnd
                        )
                        spanStyle?.let { style ->
                            resultBuilder.addStyle(style, spanStart, spanEnd)
                        }
                    }
                }
            }
        }
    return resultBuilder.toAnnotatedString()
}

private fun Annotation.matchingStringAnnotation(): StringAnnotation? =
    StringAnnotation.entries.firstOrNull { it.value == key }

private fun Annotation.toLinkAnnotation(
    onClickableTextClick: (String) -> Unit,
    style: SpanStyle?,
    pressedLinkColor: Color,
): LinkAnnotation.Url {
    val url = value
    return LinkAnnotation.Url(
        url = url,
        styles = style.toTextLinkStyles(pressedLinkColor),
        linkInteractionListener = LinkInteractionListener {
            onClickableTextClick(url)
        }
    )
}

private fun SpanStyle?.toTextLinkStyles(pressedLinkColor: Color): TextLinkStyles? =
    this?.let { style ->
        TextLinkStyles(
            style = style,
            pressedStyle = style.copy(color = pressedLinkColor)
        )
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
