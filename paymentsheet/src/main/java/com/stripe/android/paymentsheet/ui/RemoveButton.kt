package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.stripe.android.common.ui.LoadingIndicator
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.getBorderStrokeWidth
import com.stripe.android.uicore.getComposeTextStyle
import com.stripe.android.uicore.stripeShapes

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun RemoveButton(
    title: ResolvableString,
    borderColor: Color,
    idle: Boolean,
    removing: Boolean,
    onRemove: () -> Unit,
    testTag: String,
) {
    CompositionLocalProvider(
        LocalContentAlpha provides if (removing) ContentAlpha.disabled else ContentAlpha.high,
        LocalRippleConfiguration provides RippleConfiguration(color = MaterialTheme.colors.error)
    ) {
        Box(
            modifier = Modifier
                .testTag(testTag)
                .fillMaxWidth()
        ) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                TextButton(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .height(height = dimensionResource(id = R.dimen.stripe_paymentsheet_primary_button_height)),
                    border = BorderStroke(
                        width = MaterialTheme.getBorderStrokeWidth(isSelected = false),
                        color = borderColor,
                    ),
                    shape = MaterialTheme.stripeShapes.roundedCornerShape,
                    enabled = idle && !removing,
                    onClick = onRemove,
                ) {
                    Text(
                        text = title.resolve(LocalContext.current),
                        color = MaterialTheme.colors.error.copy(
                            LocalContentAlpha.current
                        ),
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
                        ),
                    color = MaterialTheme.colors.error,
                )
            }
        }
    }
}
