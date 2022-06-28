package com.stripe.example.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
internal fun DefaultExampleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = if (darkTheme) darkColors() else lightColors(),
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colors.onSurface
            ) {
                content()
            }
        }
    )
}
