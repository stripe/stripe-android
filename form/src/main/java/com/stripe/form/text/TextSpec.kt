package com.stripe.form.text

import androidx.annotation.RestrictTo
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import com.stripe.form.ContentSpec

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class TextSpec(
    val text: AnnotatedString
) : ContentSpec {
    constructor(text: String) : this(AnnotatedString(text))

    @Composable
    override fun Content(modifier: Modifier) {
        Text(
            modifier = Modifier,
            text = text
        )
    }
}
