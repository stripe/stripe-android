package com.stripe.android.uicore.text

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HtmlTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `annotatedStringResource applies inline styles used by Html screenshots`() {
        var bold = AnnotatedString("")
        var italic = AnnotatedString("")
        var underline = AnnotatedString("")
        composeTestRule.setContent {
            bold = annotatedStringResource(text = "this is some <b>bold</b> text")
            italic = annotatedStringResource(text = "this is some <i>italic</i> text")
            underline = annotatedStringResource(text = "this is some <u>underline</u> text")
        }

        composeTestRule.runOnIdle {
            assertThat(bold.spanStyleFor("bold").fontWeight).isEqualTo(FontWeight.Bold)
            assertThat(italic.spanStyleFor("italic").fontStyle).isEqualTo(FontStyle.Italic)
            assertThat(underline.spanStyleFor("underline").textDecoration).isEqualTo(TextDecoration.Underline)
        }
    }

    @Test
    fun `annotatedStringResource keeps anchor annotations used by Html screenshots`() {
        val annotatedString = annotatedStringFor("this is some <a href='stripe.com'>link</a> text")
        val links = annotatedString.getStringAnnotations("URL", 0, annotatedString.length)

        assertThat(links).hasSize(1)
        assertThat(links.single().item).isEqualTo("stripe.com")
        assertThat(annotatedString.text.substring(links.single().start, links.single().end)).isEqualTo("link")
    }

    @Test
    fun `annotatedStringResource keeps list items used by Html screenshots`() {
        val annotatedString = annotatedStringFor(
            "these are some list items: <li>item1</li><li>item2</li><li>item3</li>"
        )

        assertThat(annotatedString.text).contains("\u2022\titem1")
        assertThat(annotatedString.text).contains("\u2022\titem2")
        assertThat(annotatedString.text).contains("\u2022\titem3")
    }

    @Test
    fun `annotatedStringResource keeps promo links aligned`() {
        val annotatedString = annotatedStringFor(
            "Get \$5 back when you pay by bank. " +
                "<a href=\"https://link.com/promotion-terms\">See&#160;terms</a>"
        )
        val link = annotatedString.getStringAnnotations("URL", 0, annotatedString.length).single()

        assertThat(link.item).isEqualTo("https://link.com/promotion-terms")
        assertThat(annotatedString.text.substring(link.start, link.end)).isEqualTo("See\u00a0terms")
    }

    @Test
    fun `annotatedStringResource keeps link annotations aligned after list items`() {
        val html = "<p>Read <a href=\"https://example.com/terms\">terms</a>.</p>" +
            "<ul><li>One</li><li>Two</li></ul>" +
            "<p>Please visit <a href=\"https://support.link.com/topics/crypto\">our support page</a>.</p>"

        var annotatedString = AnnotatedString("")
        composeTestRule.setContent {
            annotatedString = annotatedStringResource(text = html)
        }

        composeTestRule.runOnIdle {
            val links = annotatedString.getStringAnnotations("URL", 0, annotatedString.length)

            assertThat(
                links.associate {
                    it.item to annotatedString.text.substring(it.start, it.end)
                }
            ).containsAtLeast(
                "https://example.com/terms",
                "terms",
                "https://support.link.com/topics/crypto",
                "our support page",
            )
            assertThat(annotatedString.text).contains("\u2022\tOne")
        }
    }

    private fun annotatedStringFor(html: String): AnnotatedString {
        var annotatedString = AnnotatedString("")
        composeTestRule.setContent {
            annotatedString = annotatedStringResource(text = html)
        }
        return composeTestRule.runOnIdle { annotatedString }
    }

    private fun AnnotatedString.spanStyleFor(text: String) = spanStyles.single {
        this.text.substring(it.start, it.end) == text
    }.item
}
