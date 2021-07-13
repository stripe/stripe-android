package com.stripe.android.paymentsheet.elements

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.GrayLight

internal data class CardStyle(
    val cardBorderColor: Color = Color(0x14000000),
    val cardBorderWidth: Dp = 1.dp,
    val cardElevation: Dp = 1.dp,
)

/**
 * This is a simple section that holds content in a card view.  It has a label, content specified
 * by the caller, and an error string.
 */
@ExperimentalAnimationApi
@Composable
internal fun Section(
    @StringRes title: Int?,
    error: String?,
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    val cardStyle = CardStyle()
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        title?.let {
            Text(
                text = stringResource(title)
            )
        }
        Card(
            border = BorderStroke(cardStyle.cardBorderWidth, cardStyle.cardBorderColor),
            elevation = cardStyle.cardElevation,
            backgroundColor = if (enabled) {
                MaterialTheme.colors.surface
            } else {
                GrayLight
            }
        ) {
            Column {
                content()
            }
        }
        AnimatedVisibility(error != null) {
            SectionError(error ?: "")
        }
    }
}

/**
 * This is how error string for the section are displayed.
 */
@Composable
internal fun SectionError(error: String) {
    Text(
        text = error,
        color = MaterialTheme.colors.error
    )
}
