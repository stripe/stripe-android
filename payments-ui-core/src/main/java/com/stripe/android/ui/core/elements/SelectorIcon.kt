package com.stripe.android.ui.core.elements

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("next_action_spec")
internal data class SelectorIcon internal constructor(
    @SerialName("light_theme_png") val lightThemePng: String? = null,
    @SerialName("dark_theme_png") val darkThemePng: String? = null,
)
