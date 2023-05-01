package com.stripe.android.financialconnections.utils

import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(ParameterizedRobolectricTestRunner::class)
class MarkdownParserTest(
    private val paramOne: String,
    private val paramTwo: String
) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters
        fun data() = listOf(
            arrayOf(
                "this **is** some markdown with a [Link text Here](https://link-url-here.org)",
                "this <b>is</b> some markdown with a <a href=\"https://link-url-here.org\">Link text Here</a>"
            ),
            arrayOf("**hola**", "<b>hola</b>"),
            arrayOf("Hello **world**", "Hello <b>world</b>"),
            arrayOf("**Bold** text", "<b>Bold</b> text"),
            arrayOf("This is *not bold**", "This is *not bold**"),
            arrayOf("**bold** text **bold**", "<b>bold</b> text <b>bold</b>"),
            arrayOf("**bold** **bold**", "<b>bold</b> <b>bold</b>")
        )
    }

    @Test
    fun toSpanned() {
        Truth.assertThat(MarkdownParser.toHtml(paramOne)).isEqualTo(paramTwo)
    }
}
