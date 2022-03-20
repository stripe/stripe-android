package com.stripe.android.ui.core.elements

import android.graphics.Typeface
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import android.widget.TextView
import androidx.annotation.RestrictTo
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import com.google.accompanist.flowlayout.FlowCrossAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import com.stripe.android.ui.core.R


@Composable
@Preview
internal fun affirmPreview() {
    AffirmElementUI()
}

/**
 * Load a styled string resource with formatting.
 *
 * @param id the resource identifier
 * @param formatArgs the format arguments
 * @return the string data associated with the resource
 */
@Composable
fun annotatedStringResource(text: String, vararg formatArgs: Any) {
    FlowRow(
        modifier = Modifier.padding(4.dp, 8.dp, 4.dp, 4.dp),
        crossAxisAlignment = FlowCrossAxisAlignment.Center
    ) {
        val spanned = remember(text) {
            HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }

        buildAnnotatedString {
            var currentStart = 0
            spanned.getSpans(0, spanned.length, Any::class.java).forEach { span ->
                val start = spanned.getSpanStart(span)
                val end = spanned.getSpanEnd(span)
                if(currentStart < start){
                    TextView(spanned.toString().substring(currentStart, start))
                }
                when (span) {
                    is StyleSpan -> when (span.style) {
                        Typeface.BOLD -> {

                            append(spanned.toString().substring(currentStart, end))
                            currentStart = end
                            addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                        }
                        Typeface.ITALIC -> {
                            append(spanned.toString().substring(currentStart, end))
                            currentStart = end
                            addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                        }
                        Typeface.BOLD_ITALIC -> {
                            append(spanned.toString().substring(currentStart, end))
                            currentStart = end
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
                        append(spanned.toString().substring(currentStart, end))
                        currentStart = end
                        addStyle(
                            SpanStyle(textDecoration = TextDecoration.Underline),
                            start,
                            end
                        )
                    }
                    is ForegroundColorSpan -> {
                        append(spanned.toString().substring(currentStart, end))
                        currentStart = end
                        addStyle(SpanStyle(color = Color(span.foregroundColor)), start, end)
                    }
                    is ImageSpan -> {
                        append(spanned.toString().substring(currentStart, start))
                        currentStart = end
                        appendInlineContent("affirmId")
                    }
                    is URLSpan -> {
                        append(spanned.toString().substring(currentStart, end))
                        currentStart = end
                        addStyle(
                            style = TextStyle.Default.toSpanStyle(),
                            start = start,
                            end = end
                        )
                        addStringAnnotation(
                            tag = "link",
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun AffirmElementUI() {

    val inlineContentMap = mapOf(
        "affirmId" to InlineTextContent(
            Placeholder(
                44.sp,
                20.sp,
                PlaceholderVerticalAlign.Bottom
            )
        ) {
            Image(
                painter = if (isSystemInDarkTheme()) {
                    painterResource(R.drawable.stripe_ic_affirm_logo_dark)
                } else {
                    painterResource(R.drawable.stripe_ic_affirm_logo_light)
                },
                contentDescription = stringResource(
                    R.string.affirm_buy_now_pay_later
                )
            )
        }
    )

    Text(
        annotatedStringResource(
            "<b>Buy</b> now, <a href=\"https://google.com\">pay</a> later with <img src=\"affirm\"/>."
        ), inlineContent = inlineContentMap
    )
    ClickableText(
        annotatedText,
        modifier = modifier,
        style = style,
        softWrap = softWrap,
//        overflow = overflow,
//        maxLines = maxLines,
        onTextLayout = onTextLayout
    ) {
        annotatedText.getStringAnnotations(tag = Tag.Hyperlink.name, start = it, end = it)
            .firstOrNull()?.let {
                onHyperlinkClick(it.item)
            }
    }
//
//    val affirmImage = if (isSystemInDarkTheme()) {
//        LocalContext.current.resources.getDrawable(R.drawable.stripe_ic_affirm_logo_dark)
//
//    } else {
//        LocalContext.current.resources.getDrawable(R.drawable.stripe_ic_affirm_logo_light)
//    }
//
//    Log.e("MLB", "Image height: ${affirmImage.minimumHeight}")
//
//    affirmImage.setBounds(
//        0,
//        0,
////        -1 * affirmImage.minimumHeight /2,
//        affirmImage.minimumWidth,
//        affirmImage.minimumHeight
//    )
//    HtmlText(
//        "Buy now, pay later with <img src=\"affirm\"/>.",
////            stringResource(R.string.affirm_buy_now_pay_later),
//        {
//            affirmImage
//        },
//        { _, _, _, _ ->
//        },
////            color = PaymentsTheme.colors.colorTextSecondary
//    )
//    FlowRow(
//        modifier = Modifier.padding(4.dp, 8.dp, 4.dp, 4.dp)
//            .a
//        ,
//    ) {

//        val bnplMessage =
//        val logoPresent = bnplMessage.contains("<>")
//
//        var beforeLogoString: String? = null
//        var afterLogoString: String? = null
//        if (logoPresent) {
//            beforeLogoString = bnplMessage.split("%s")[0]
//            afterLogoString = bnplMessage.split("%s")[1]
//        }
//
//        beforeLogoString?.let {
//            Text(
//                it,
//                Modifier
//                    .padding(end = 4.dp)
//                    .padding(top = 6.dp),
//                color = PaymentsTheme.colors.colorTextSecondary
//            )
//        }
//        if (logoPresent) {
//            Image(
//                painter = if (isSystemInDarkTheme()) {
//                    painterResource(R.drawable.stripe_ic_affirm_logo_dark)
//                } else {
//                    painterResource(R.drawable.stripe_ic_affirm_logo_light)
//                },
//                contentDescription = stringResource(
//                    R.string.affirm_buy_now_pay_later
//                )
//            )
//        }
//        afterLogoString?.let {
//            Text(
//                it,
//                Modifier
//                    .padding(end = 4.dp)
//                    .padding(top = 6.dp),
//                color = PaymentsTheme.colors.colorTextSecondary
//            )
//
//        }
//    }
}
