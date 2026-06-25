package com.stripe.android.common.nfcscan.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R

private val CoilCircleSize = 200.dp
private val CoilIconSize = 96.dp
private val TopTextOffset = (-55).dp
private val ShadowElevation = 8.dp

@Composable
internal fun NfcCoil(
    shouldRenderTextAboveCoil: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier.width(IntrinsicSize.Min)) {
        if (shouldRenderTextAboveCoil) {
            NfcCoilInstructionText(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offsetWithGap(TopTextOffset)
            )
        }

        val isDarkTheme = isSystemInDarkTheme()

        val shadow = if (isDarkTheme) {
            Modifier
        } else {
            Modifier.shadow(elevation = ShadowElevation, shape = CircleShape)
        }

        Box(
            modifier = Modifier
                .size(CoilCircleSize)
                .then(shadow)
                .background(color = MaterialTheme.colors.primary, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            NfcCoilIcon(modifier = Modifier.size(CoilIconSize))
        }

        if (!shouldRenderTextAboveCoil) {
            NfcCoilInstructionText(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offsetWithGap(30.dp)
            )
        }
    }
}

@Composable
private fun NfcCoilIcon(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Image(
            painter = painterResource(R.drawable.stripe_ic_material_nfc_coil_circle),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        Image(
            painter = painterResource(R.drawable.stripe_ic_material_nfc_coil_bar1),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
        Image(
            painter = painterResource(R.drawable.stripe_ic_material_nfc_coil_bar2),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        Image(
            painter = painterResource(R.drawable.stripe_ic_material_nfc_coil_bar3),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun NfcCoilInstructionText(
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(R.string.stripe_nfc_scan_hold_card_behind_phone),
        modifier = modifier
            .wrapContentSize(unbounded = true),
        color = MaterialTheme.colors.onSurface,
        style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.Bold),
        textAlign = TextAlign.Center,
    )
}

private fun Modifier.offsetWithGap(gap: Dp): Modifier {
    return layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)

        layout(placeable.width, 0) {
            val spacingGap = gap.roundToPx()
            placeable.placeRelative(0, spacingGap)
        }
    }
}
