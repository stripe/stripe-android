package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.core.strings.ResolvableString

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AddressTextFieldConfig(
    label: ResolvableString,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Words,
    keyboard: KeyboardType = KeyboardType.Text,
    optional: Boolean = false,
    val autofillContentType: ContentType? = null,
) : SimpleTextFieldConfig(
    label = label,
    capitalization = capitalization,
    keyboard = keyboard,
    optional = optional,
    allowsEmojis = false,
)
