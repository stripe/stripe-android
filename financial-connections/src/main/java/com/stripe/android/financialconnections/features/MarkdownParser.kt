package com.stripe.android.financialconnections.features

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

    internal fun toHtml(string: String): String {
        var newText = string
        markDownToHtmlRegex.forEach { (regex, replacement) ->
            newText = newText.replace(regex, replacement)
        }
        return newText
    }
}
