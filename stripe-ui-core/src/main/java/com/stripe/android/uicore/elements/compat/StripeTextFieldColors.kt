/*
 * Copyright 2024 Stripe, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stripe.android.uicore.elements.compat

import androidx.annotation.RestrictTo
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color

/**
 * Custom TextField colors class for Stripe that wraps M3's [TextFieldColors] and exposes
 * color accessors publicly. This is needed because Material3's TextFieldColors has internal
 * color accessor methods.
 *
 * @property m3Colors The underlying Material3 TextFieldColors, used for M3 APIs like indicatorLine
 */
@Immutable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class StripeTextFieldColors(
    val m3Colors: TextFieldColors
) {
    @Composable
    fun textColor(enabled: Boolean, isError: Boolean, interactionSource: InteractionSource): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()
        val targetValue = when {
            !enabled -> m3Colors.disabledTextColor
            isError -> m3Colors.errorTextColor
            focused -> m3Colors.focusedTextColor
            else -> m3Colors.unfocusedTextColor
        }
        return rememberUpdatedState(targetValue)
    }

    @Composable
    fun containerColor(enabled: Boolean, isError: Boolean, interactionSource: InteractionSource): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()
        val targetValue = when {
            !enabled -> m3Colors.disabledContainerColor
            isError -> m3Colors.errorContainerColor
            focused -> m3Colors.focusedContainerColor
            else -> m3Colors.unfocusedContainerColor
        }
        return rememberUpdatedState(targetValue)
    }

    @Composable
    fun cursorColor(isError: Boolean): Color {
        return if (isError) m3Colors.errorCursorColor else m3Colors.cursorColor
    }

    @Composable
    fun leadingIconColor(enabled: Boolean, isError: Boolean, interactionSource: InteractionSource): Color {
        val focused by interactionSource.collectIsFocusedAsState()
        return when {
            !enabled -> m3Colors.disabledLeadingIconColor
            isError -> m3Colors.errorLeadingIconColor
            focused -> m3Colors.focusedLeadingIconColor
            else -> m3Colors.unfocusedLeadingIconColor
        }
    }

    @Composable
    fun trailingIconColor(enabled: Boolean, isError: Boolean, interactionSource: InteractionSource): Color {
        val focused by interactionSource.collectIsFocusedAsState()
        return when {
            !enabled -> m3Colors.disabledTrailingIconColor
            isError -> m3Colors.errorTrailingIconColor
            focused -> m3Colors.focusedTrailingIconColor
            else -> m3Colors.unfocusedTrailingIconColor
        }
    }

    @Composable
    fun labelColor(enabled: Boolean, isError: Boolean, interactionSource: InteractionSource): Color {
        val focused by interactionSource.collectIsFocusedAsState()
        return when {
            !enabled -> m3Colors.disabledLabelColor
            isError -> m3Colors.errorLabelColor
            focused -> m3Colors.focusedLabelColor
            else -> m3Colors.unfocusedLabelColor
        }
    }

    @Composable
    fun placeholderColor(enabled: Boolean): Color {
        return if (enabled) m3Colors.unfocusedPlaceholderColor else m3Colors.disabledPlaceholderColor
    }
}

/**
 * Default colors for Stripe text fields, following Material3 design.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object StripeTextFieldDefaults {
    @Composable
    fun colors(
        focusedTextColor: Color = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor: Color = MaterialTheme.colorScheme.onSurface,
        disabledTextColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        errorTextColor: Color = MaterialTheme.colorScheme.onSurface,
        focusedContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
        unfocusedContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
        disabledContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
        errorContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
        cursorColor: Color = MaterialTheme.colorScheme.primary,
        errorCursorColor: Color = MaterialTheme.colorScheme.error,
        focusedIndicatorColor: Color = MaterialTheme.colorScheme.primary,
        unfocusedIndicatorColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledIndicatorColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        errorIndicatorColor: Color = MaterialTheme.colorScheme.error,
        focusedLeadingIconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedLeadingIconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLeadingIconColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        errorLeadingIconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedTrailingIconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedTrailingIconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledTrailingIconColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        errorTrailingIconColor: Color = MaterialTheme.colorScheme.error,
        focusedLabelColor: Color = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLabelColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        errorLabelColor: Color = MaterialTheme.colorScheme.error,
        focusedPlaceholderColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedPlaceholderColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledPlaceholderColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        errorPlaceholderColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    ): StripeTextFieldColors {
        val m3Colors = TextFieldDefaults.colors(
            focusedTextColor = focusedTextColor,
            unfocusedTextColor = unfocusedTextColor,
            disabledTextColor = disabledTextColor,
            errorTextColor = errorTextColor,
            focusedContainerColor = focusedContainerColor,
            unfocusedContainerColor = unfocusedContainerColor,
            disabledContainerColor = disabledContainerColor,
            errorContainerColor = errorContainerColor,
            cursorColor = cursorColor,
            errorCursorColor = errorCursorColor,
            focusedIndicatorColor = focusedIndicatorColor,
            unfocusedIndicatorColor = unfocusedIndicatorColor,
            disabledIndicatorColor = disabledIndicatorColor,
            errorIndicatorColor = errorIndicatorColor,
            focusedLeadingIconColor = focusedLeadingIconColor,
            unfocusedLeadingIconColor = unfocusedLeadingIconColor,
            disabledLeadingIconColor = disabledLeadingIconColor,
            errorLeadingIconColor = errorLeadingIconColor,
            focusedTrailingIconColor = focusedTrailingIconColor,
            unfocusedTrailingIconColor = unfocusedTrailingIconColor,
            disabledTrailingIconColor = disabledTrailingIconColor,
            errorTrailingIconColor = errorTrailingIconColor,
            focusedLabelColor = focusedLabelColor,
            unfocusedLabelColor = unfocusedLabelColor,
            disabledLabelColor = disabledLabelColor,
            errorLabelColor = errorLabelColor,
            focusedPlaceholderColor = focusedPlaceholderColor,
            unfocusedPlaceholderColor = unfocusedPlaceholderColor,
            disabledPlaceholderColor = disabledPlaceholderColor,
            errorPlaceholderColor = errorPlaceholderColor,
        )

        return StripeTextFieldColors(m3Colors = m3Colors)
    }
}
