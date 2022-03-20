package com.stripe.android.ui.core.elements

import android.text.Html
import android.view.Gravity
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat


@Composable
fun HtmlText(
    html: String,
    imageGetter: Html.ImageGetter,
    tagHandler: Html.TagHandler,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val textView = TextView(context)
            textView.gravity = Gravity.TOP
            textView
        },
        update = {
            val htmlStr = HtmlCompat.fromHtml(
                html,
                HtmlCompat.FROM_HTML_MODE_LEGACY,
                imageGetter,
                tagHandler
            )
//            val builder = SpannableStringBuilder(htmlStr)
//            val spans: Array<out ImageSpan>? = builder.getSpans(0, builder.length, ImageSpan::class.java)
//            if (spans?.size == 1) {
//                val start: Int = builder.getSpanStart(spans[0])
//                val end: Int = builder.getSpanEnd(spans[0])
//                val span = ImageSpan(cont, ImageSpan.ALIGN_BASELINE)
//                builder.setSpan(spans[0]., start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
//            }
            it.text = htmlStr
        }
    )
}
