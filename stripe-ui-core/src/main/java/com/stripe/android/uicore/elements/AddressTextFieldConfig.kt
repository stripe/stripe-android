package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.core.strings.ResolvableString

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AddressTextFieldConfig(
    label: ResolvableString,
    capitalization: KeyboardCapitalization,
    keyboard: KeyboardType,
    optional: Boolean,
    val autofillContentType: ContentType?,
) : SimpleTextFieldConfig(label, capitalization, keyboard, optional = optional, allowsEmojis = false)
