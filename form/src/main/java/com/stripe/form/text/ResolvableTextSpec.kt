package com.stripe.form.text

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.uicore.strings.resolve
import com.stripe.form.ContentBox
import com.stripe.form.ContentSpec

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ResolvableTextSpec(
    private val text: ResolvableString
) : ContentSpec {
    constructor(@StringRes id: Int) : this(id.resolvableString)

    @Composable
    override fun Content(modifier: Modifier) {
        ContentBox(
            modifier = modifier,
            spec = TextSpec(
                text = text.resolve()
            )
        )
    }
}