package com.stripe.android.ui.core.elements

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import com.google.accompanist.flowlayout.FlowRow
import com.stripe.android.ui.core.PaymentsTheme

private const val LINK_TAG = "URL"


data class EmbeddableImage(
    @DrawableRes val id: Int,
    @StringRes val contentDescription: Int
)

/**
 * This will display html annotated text in a string.  Images cannot be embedded in
 * <a> link tags.  The following tags are supported: <a>, <b>, <u>, <i>, <img>
 * The source value in the img tab, must map to something in the imageGetter.
 */
@Composable
internal fun Html(
    html: String,
    imageGetter: Map<String, EmbeddableImage>,
    modifier: Modifier = Modifier
) {
    val inlineContentMap = imageGetter.entries.associate { (key, value) ->
        val painter = painterResource(value.id)
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
                        value.contentDescription
                    )
                )
            }
        )
    }

    val context = LocalContext.current
    val annotatedText = annotatedLinkStringResource(html, imageGetter)

    FlowRow(modifier = modifier) {
        var currentStart = 0
        // Each tag should be created as a separate element so it is read by accessibility
        // correctly.  If this is not done and there is an embedded image the name of the image will
        // be read after the rest of the string is read.
        val linkStrings = annotatedText.getStringAnnotations(0, annotatedText.length)
        linkStrings.forEach { annotateStringRange ->
            if (annotateStringRange.start > currentStart) {
                HtmlText(
                    annotatedText,
                    currentStart,
                    annotateStringRange.start - 1,
                    inlineContentMap
                )
                currentStart = annotateStringRange.start
            }
            if (annotateStringRange.tag == LINK_TAG) {
                HtmlLinkText(
                    context,
                    annotatedText,
                    annotateStringRange.start,
                    annotateStringRange.end,
                    annotateStringRange.item
                )
                currentStart = annotateStringRange.end
            } else {
                HtmlText(
                    annotatedText,
                    currentStart,
                    annotateStringRange.end,
                    inlineContentMap
                )
                currentStart = annotateStringRange.end
            }
        }
        if (currentStart < annotatedText.length) {
            HtmlText(
                annotatedText,
                currentStart,
                annotatedText.length,
                inlineContentMap
            )
            currentStart = annotatedText.length
        }
    }

}

@Composable
internal fun HtmlLinkText(
    context: Context,
    annotatedText: AnnotatedString,
    start: Int,
    end: Int,
    item: String,
) {
    ClickableText(
        text = annotatedText.subSequence(start, end),
        modifier = Modifier
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) { // makes it a separate accessible item,
                this.role = Role.Button
            },
        onClick = {
            val openURL = Intent(Intent.ACTION_VIEW)
            openURL.data = Uri.parse(item)
            context.startActivity(openURL)
        },
        style = TextStyle.Default.copy(
            color = PaymentsTheme.colors.subtitle
        ),
    )
}

@Composable
internal fun HtmlText(
    annotatedText: AnnotatedString,
    start: Int,
    end: Int,
    inlineContentMap: Map<String, InlineTextContent>
) {
    Text(
        text = annotatedText.subSequence(
            start,
            end,
        ),
        inlineContent = inlineContentMap,
        modifier = Modifier
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {}, // makes it a separate accessible item,
        color = PaymentsTheme.colors.subtitle,
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
internal fun annotatedLinkStringResource(
    text: String,
    imageGetter: Map<String, EmbeddableImage>,
    urlStyle: TextDecoration = TextDecoration.Underline
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
                if (currentStart < spanned.toString().length
                    && start < spanned.toString().length
                    && start - currentStart >= 0
                ) {
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
                            addStyle(
                                SpanStyle(textDecoration = TextDecoration.Underline),
                                start,
                                end
                            )
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
                            addStyle(
                                SpanStyle(textDecoration = urlStyle),
                                start,
                                end
                            )
                            addStringAnnotation(
                                tag = LINK_TAG,
                                annotation = span.url,
                                start = start,
                                end = end
                            )
                        }
                    }
                }
            }
            if (currentStart != spanned.toString().length) {
                append(spanned.toString().substring(currentStart))
            }
        }
    }
}

