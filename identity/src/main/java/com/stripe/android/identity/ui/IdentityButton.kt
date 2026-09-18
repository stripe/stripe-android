package com.stripe.android.identity.ui

import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun IdentityButton(
    text: String,
    uppercase: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean
) {
    val style = IdentityButtonTheme.primary
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = style.colors,
        elevation = style.elevation,
        shape = style.shape,
        modifier = modifier.then(style.heightModifier())
    ) {
        Text(text = style.text(text, uppercase))
    }
}

@Composable
internal fun IdentityOutlinedButton(
    text: String,
    uppercase: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean
) {
    val style = IdentityButtonTheme.secondary
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        border = style.border,
        colors = style.colors,
        elevation = style.elevation,
        shape = style.shape,
        modifier = modifier.then(style.heightModifier())
    ) {
        Text(text = style.text(text, uppercase))
    }
}

private fun IdentityButtonStyle.heightModifier(): Modifier {
    return height?.let { Modifier.height(it) } ?: Modifier
}
