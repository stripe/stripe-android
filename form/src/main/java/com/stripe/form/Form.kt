package com.stripe.form

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList

@Stable
data class Form(
    val content: ImmutableList<ContentSpec>,
) {
    operator fun <T> get(key: Key<T>): FormFieldSpec<T> {
        val spec = content.filterIsInstance<FormFieldSpec<T>>()
            .firstOrNull { it.state.key == key }
        return requireNotNull(spec) {
            "No field found for key ${key.key}"
        }
    }

    fun <T> onValueChange(
        key: Key<T>,
        value: T,
        isComplete: Boolean = true
    ) {
        get(key)
            .state
            .onValueChange(
                ValueChange(
                    key = key,
                    value = value,
                    isComplete = isComplete
                )
            )
    }
}
