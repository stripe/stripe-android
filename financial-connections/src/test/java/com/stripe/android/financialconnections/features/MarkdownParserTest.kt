package com.stripe.android.financialconnections.features

import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class MarkdownParserTest(
    private val paramOne: String,
    private val paramTwo: String
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(
            arrayOf(
                "this **is** some markdown with a [Link text Here](https://link-url-here.org)",
                "this <annotation bold=\"\">is</annotation> some markdown with a <annotation clickable=https://link-url-here.org>Link text Here</annotation>"
            ),
            arrayOf(
                "**hola**",
                "<annotation bold=\"\">hola</annotation>"
            ),
        )
    }

    @Test
    fun toSpanned() {
        Truth.assertThat(MarkdownParser.toSpanned(paramOne)).isEqualTo(paramTwo)
    }
}
