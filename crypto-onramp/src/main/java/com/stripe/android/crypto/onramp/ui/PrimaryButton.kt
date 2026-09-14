package com.stripe.android.crypto.onramp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stripe.android.crypto.onramp.ui.theme.OnrampTheme

@Composable
internal fun PrimaryButton(
    modifier: Modifier,
    label: String,
    state: PrimaryButtonState,
    onButtonClick: () -> Unit,
) {
    Box(modifier) {
        Button(
            onClick = onButtonClick,
            modifier = Modifier
                .height(OnrampTheme.shapes.primaryButtonHeight)
                .fillMaxWidth()
                .testTag(PrimaryButtonTag),
            enabled = state == PrimaryButtonState.Enabled,
            elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
            shape = OnrampTheme.shapes.primaryButton,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = OnrampTheme.colors.buttonBrand,
                contentColor = OnrampTheme.colors.onButtonBrand,
                disabledBackgroundColor = OnrampTheme.colors.buttonBrand,
                disabledContentColor = OnrampTheme.colors.onButtonBrand.copy(alpha = ContentAlpha.disabled),
            ),
        ) {
            if (state == PrimaryButtonState.Processing) {
                OnrampSpinner(
                    modifier = Modifier.size(20.dp).testTag(ProgressIndicatorTestTag),
                    backgroundColor = OnrampTheme.colors.surfaceBackdrop.copy(alpha = 0.1f),
                    strokeWidth = 4.dp,
                    filledColor = OnrampTheme.colors.onButtonBrand,
                )
            } else {
                Text(
                    text = label,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp),
                    color = OnrampTheme.colors.onButtonBrand.copy(
                        alpha = if (state == PrimaryButtonState.Disabled) ContentAlpha.disabled else ContentAlpha.high,
                    ),
                    textAlign = TextAlign.Center,
                    style = OnrampTheme.typography.bodyEmphasized,
                )
            }
        }
    }
}

internal enum class PrimaryButtonState {
    Enabled,
    Disabled,
    Processing,
}

internal const val PrimaryButtonTag = "OnrampPrimaryButton"
internal const val ProgressIndicatorTestTag = "OnrampProgressIndicator"
