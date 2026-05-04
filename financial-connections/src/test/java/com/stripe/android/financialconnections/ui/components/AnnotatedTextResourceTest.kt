package com.stripe.android.financialconnections.ui.components

import android.text.SpannableString
import android.text.Spanned
import android.text.style.URLSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration
import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class AnnotatedTextResourceTest {

    @Test
    fun `buildAnnotatedStringResource converts URL spans into link annotations`() {
        val linkText = "Access Data"
        val expectedUrl = "stripe://data-access-notice"
        val resource = SpannableString("$linkText takes 1-2 business days").apply {
            setSpan(
                URLSpan(expectedUrl),
                0,
                linkText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        val linkStyle = SpanStyle(textDecoration = TextDecoration.Underline)
        var clickedUrl: String? = null

        val annotatedString = buildAnnotatedStringResource(
            resource = resource,
            onClickableTextClick = { clickedUrl = it },
            annotationStyles = mapOf(StringAnnotation.CLICKABLE to linkStyle),
            pressedLinkColor = Color.Red,
        )

        val link = annotatedString.getLinkAnnotations(0, annotatedString.length).single()
        assertThat(link.start).isEqualTo(0)
        assertThat(link.end).isEqualTo(linkText.length)
        assertThat(link.item).isInstanceOf(LinkAnnotation.Url::class.java)

        val linkAnnotation = link.item as LinkAnnotation.Url
        assertThat(linkAnnotation.url).isEqualTo(expectedUrl)
        assertThat(linkAnnotation.styles).isEqualTo(
            TextLinkStyles(
                style = linkStyle,
                pressedStyle = linkStyle.copy(color = Color.Red)
            )
        )

        linkAnnotation.linkInteractionListener?.onClick(linkAnnotation)

        assertThat(clickedUrl).isEqualTo(expectedUrl)
    }
}
