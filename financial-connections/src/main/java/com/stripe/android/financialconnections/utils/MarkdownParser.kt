package com.stripe.android.financialconnections.utils

internal object MarkdownParser {

    private const val REGEX_BOLD_ASTERISKS = "\\*\\*(.*?)\\*\\*"
    private const val REGEX_BOLD_UNDERSCORES = "__([^_]+)__"

    private const val REGEX_LINK = "\\[([^]]+)]\\(([^)]+)\\)"

    private val markDownToHtmlRegex = listOf<Pair<Regex, (MatchResult) -> CharSequence>>(
        // bold, italics rules
        REGEX_BOLD_ASTERISKS.toRegex() to { "<b>${it.groupValues[1]}</b>" },
        REGEX_BOLD_UNDERSCORES.toRegex() to { "<b>${it.groupValues[1]}</b>" },

        // links
        REGEX_LINK.toRegex() to { "<a href=\"${it.groupValues[2]}\">${it.groupValues[1]}</a>" }
    )

    internal fun toHtml(string: String): String {
        var newText = string
        markDownToHtmlRegex.forEach { (regex, replacement) ->
            newText = regex.replace(newText, replacement)
        }
        return newText
    }
}
