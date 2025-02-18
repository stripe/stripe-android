package com.stripe.form

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList

@Stable
data class Form(
    val content: ImmutableList<ContentSpec>
)
