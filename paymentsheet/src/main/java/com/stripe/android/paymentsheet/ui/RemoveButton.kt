package com.stripe.android.paymentsheet.ui

import androidx.annotation.RestrictTo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.RippleDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.stripe.android.common.ui.LoadingIndicator
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.getComposeTextStyle

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val REMOVE_BUTTON_LOADING = "REMOVE_BUTTON_LOADING"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RemoveButton(
    title: ResolvableString,
    borderColor: Color,
    idle: Boolean,
    removing: Boolean,
    onRemove: () -> Unit,
    testTag: String,
) {
    val shape = PrimaryButtonTheme.shape
    CompositionLocalProvider(
        LocalContentColor provides if (removing) {
            MaterialTheme.colorScheme.error.copy(alpha = 0.38f)
        } else {
            MaterialTheme.colorScheme.error
        },
        LocalRippleConfiguration provides ErrorRippleConfiguration,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                TextButton(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag(testTag)
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = shape.height),
                    border = BorderStroke(
                        width = shape.borderStrokeWidth.coerceAtLeast(2.dp),
                        color = borderColor,
                    ),
                    shape = RoundedCornerShape(shape.cornerRadius),
                    enabled = idle && !removing,
                    onClick = onRemove,
                ) {
                    Text(
                        text = title.resolve(LocalContext.current),
                        color = LocalContentColor.current,
                        style = StripeTheme.primaryButtonStyle.getComposeTextStyle(),
                    )
                }
            }

            if (removing) {
                LoadingIndicator(
                    modifier = Modifier.align(Alignment.CenterEnd)
                        .padding(
                            start = 8.dp,
                            end = 8.dp
                        )
                        .testTag(REMOVE_BUTTON_LOADING),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private val ErrorRippleConfiguration: RippleConfiguration
    @Composable
    get() = RippleConfiguration(
        color = MaterialTheme.colorScheme.error,
        rippleAlpha = RippleDefaults.RippleAlpha
    )
