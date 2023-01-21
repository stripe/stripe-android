package com.stripe.android.financialconnections.features

internal object MarkdownParser {

    private val markDownToHtmlRegex = listOf<Pair<Regex, (MatchResult) -> CharSequence>>(
        // bold, italics rules
        "\\*\\*(.*?)\\*\\*".toRegex() to { "<b>${it.groupValues[1]}</b>" },
        "__([^_]+)__".toRegex() to { "<b>${it.groupValues[1]}</b>" },

        // links
        "\\[([^]]+)]\\(([^)]+)\\)".toRegex() to { "<a href=\"${it.groupValues[2]}\">${it.groupValues[1]}</a>" }
    )

    internal fun toHtml(string: String): String {
        var newText = string
        markDownToHtmlRegex.forEach { (regex, replacement) ->
            newText = regex.replace(newText, replacement)
        }
        return newText
    }
}
