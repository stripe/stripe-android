package com.stripe.android.financialconnections.ui

import androidx.compose.ui.Modifier

/**
 * Conditionally applies the given [Modifier] functions based on the [condition].
 */
internal inline fun Modifier.conditional(
    condition: Boolean,
    ifTrue: Modifier.() -> Modifier,
    ifFalse: Modifier.() -> Modifier = { this },
): Modifier = if (condition) {
    then(ifTrue(Modifier))
} else {
    then(ifFalse(Modifier))
}