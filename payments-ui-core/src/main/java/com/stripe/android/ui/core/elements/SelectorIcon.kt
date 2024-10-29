package com.stripe.android.ui.core.elements

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("next_action_spec")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class SelectorIcon(
    @SerialName("light_theme_png") val lightThemePng: String? = null,
    @SerialName("dark_theme_png") val darkThemePng: String? = null,
) : Parcelable
