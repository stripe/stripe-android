package com.stripe.android.financialconnections.features

import android.os.Build
import android.text.Html
import android.text.Spanned


internal object MarkdownParser {
    private val markDownToHtmlRegex = listOf(
        // bold, italics rules
        "\\*\\*\\s?([^\\n]+)\\*\\*".toRegex() to "<b>$1</b>",
        "\\*\\s?([^\\n]+)\\*".toRegex() to "<i>$1</i>",
        "__([^_]+)__".toRegex() to "<b>$1</b>",
        "_([^_]+)_".toRegex() to "<i>$1</i>",

        // links
        "\\[([^]]+)]\\(([^)]+)\\)".toRegex() to "<a href=\"$2\">$1</a>"
    )

    fun toSpanned(text: String): Spanned {
        var newText = text
        markDownToHtmlRegex.forEach { (regex, replacement) ->
            newText = newText.replace(regex, replacement)
        }
        return fromHtml(newText)
    }

    @SuppressWarnings("deprecation")
    private fun fromHtml(source: String?): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(source)
        }
    }
}
