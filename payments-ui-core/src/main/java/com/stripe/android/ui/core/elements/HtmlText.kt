package com.stripe.android.ui.core.elements

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import com.stripe.android.ui.core.PaymentsTheme

private const val LINK_TAG = "URL"

@Composable
internal fun HtmlText(
    html: String,
    imageGetter: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val inlineContentMap = imageGetter.entries.associate { (key, value) ->
        val painter = painterResource(value)
        val height = painter.intrinsicSize.height
        val width = painter.intrinsicSize.width
        val newWidth = MaterialTheme.typography.body1.fontSize * (width / height)

        key to InlineTextContent(
            Placeholder(
                newWidth,
                MaterialTheme.typography.body1.fontSize,
                PlaceholderVerticalAlign.AboveBaseline
            ),
            children = {
                Image(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    painter = painter,
                    contentDescription = stringResource(
                        value
                    )
                )
            }
        )
    }

    val annotatedText = annotatedStringResource(html, imageGetter)

    val context = LocalContext.current
    ClickableText(
        annotatedText,
        inlineContent = inlineContentMap,
        modifier = modifier
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {}, // makes it a separate accessibile item,
        color = PaymentsTheme.colors.subtitle,
        onClick = {
            annotatedText
                .getStringAnnotations(LINK_TAG, it, it)
                .firstOrNull()?.let { annotation ->
                    val openURL = Intent(Intent.ACTION_VIEW)
                    openURL.data = Uri.parse(annotation.item)
                    context.startActivity(openURL)
                }
        }
    )
}

/**
 * Load a styled string resource with formatting.
 *
 * @param text the html text
 * @param imageGetter the mapping of string to resource id
 * @return the string data associated with the resource
 */
@Composable
private fun annotatedStringResource(
    text: String,
    imageGetter: Map<String, Int>,
): AnnotatedString {
    val spanned = remember(text) {
        HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
    return remember(spanned) {
        buildAnnotatedString {
            var currentStart = 0
            spanned.getSpans(0, spanned.length, Any::class.java).forEach { span ->
                val start = spanned.getSpanStart(span)
                val end = spanned.getSpanEnd(span)
                append(spanned.toString().substring(currentStart, start))
                currentStart = start
                when (span) {
                    is StyleSpan -> when (span.style) {
                        Typeface.BOLD -> {
                            addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                        }
                        Typeface.ITALIC -> {
                            addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                        }
                        Typeface.BOLD_ITALIC -> {
                            addStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontStyle = FontStyle.Italic,
                                ),
                                start,
                                end,
                            )
                        }
                    }
                    is UnderlineSpan -> {
                        addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
                    }
                    is ForegroundColorSpan -> {
                        addStyle(SpanStyle(color = Color(span.foregroundColor)), start, end)
                    }
                    is ImageSpan -> {
                        currentStart = end
                        span.source?.let {
                            requireNotNull(imageGetter.containsKey(span.source!!))
                            appendInlineContent(span.source!!)
                        }
                    }
                    is URLSpan -> {
                        addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
                        addStringAnnotation(
                            tag = LINK_TAG,
                            annotation = span.url,
                            start = start,
                            end = end
                        )
                    }
                }
            }
            if (currentStart != spanned.toString().length) {
                append(spanned.toString().substring(currentStart))
            }
        }
    }
}

@Composable
private fun ClickableText(
    text: AnnotatedString,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = TextStyle.Default,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    onClick: (Int) -> Unit
) {
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    val pressIndicator = Modifier.pointerInput(onClick) {
        detectTapGestures { pos ->
            layoutResult.value?.let { layoutResult ->
                onClick(layoutResult.getOffsetForPosition(pos))
            }
        }
    }

    BasicText(
        text = text,
        modifier = modifier.then(pressIndicator),
        style = style.copy(
            color = color
        ),
        softWrap = softWrap,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = {
            layoutResult.value = it
            onTextLayout(it)
        },
        inlineContent = inlineContent
    )
}
