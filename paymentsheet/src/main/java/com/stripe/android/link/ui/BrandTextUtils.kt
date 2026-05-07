package com.stripe.android.link.ui

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString

internal fun String.buildBrandIconAnnotatedString(
    brandToken: String,
    inlineContentId: String,
    alternateText: String = "[icon]",
): AnnotatedString = buildAnnotatedString {
    val brandIndex = indexOf(brandToken)

    if (brandToken.isEmpty() || brandIndex < 0) {
        append(this@buildBrandIconAnnotatedString)
        return@buildAnnotatedString
    }

    append(substring(0, brandIndex))
    appendInlineContent(id = inlineContentId, alternateText = alternateText)
    append(substring(brandIndex + brandToken.length))
}
