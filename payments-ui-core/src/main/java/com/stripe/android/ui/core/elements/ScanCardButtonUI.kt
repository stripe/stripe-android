package com.stripe.android.ui.core.elements

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.stripecardscan.cardscan.CardScanConfiguration
import com.stripe.android.stripecardscan.cardscan.CardScanSheetResult
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.cardscan.CardScanContract
import com.stripe.android.ui.core.cardscan.CardScanGoogleLauncher.Companion.rememberCardScanGoogleLauncher
import com.stripe.android.ui.core.cardscan.CardScanResult
import com.stripe.android.uicore.IconStyle
import com.stripe.android.uicore.LocalIconStyle
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun ScanCardButtonUI(
    enabled: Boolean,
    elementsSessionId: String?,
    onGoogleCardScanResult: (CardScanResult) -> Unit,
    onResult: (CardScanSheetResult) -> Unit
) {
    val cardScanLauncher =
        rememberLauncherForActivityResult(CardScanContract()) {
            onResult(it)
        }

    val context = LocalContext.current
    val cardScanGoogleLauncher = if (FeatureFlags.cardScanGooglePayMigration.isEnabled) {
        rememberCardScanGoogleLauncher(context, onGoogleCardScanResult)
    } else {
        null
    }
    val isCardScanGoogleAvailable by if (FeatureFlags.cardScanGooglePayMigration.isEnabled) {
        cardScanGoogleLauncher?.isAvailable?.collectAsState() ?: remember { mutableStateOf(false) }
    } else {
        remember { mutableStateOf(false) }
    }

    if (!FeatureFlags.cardScanGooglePayMigration.isEnabled || isCardScanGoogleAvailable)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            enabled = enabled,
            onClick = {
                if (FeatureFlags.cardScanGooglePayMigration.isEnabled) {
                    cardScanGoogleLauncher?.launch(context)
                } else {
                    cardScanLauncher.launch(
                        input = CardScanContract.Args(
                            configuration = CardScanConfiguration(
                                elementsSessionId = elementsSessionId
                            )
                        )
                    )
                }
            }
        )
    ) {
        val iconStyle = LocalIconStyle.current

        val icon = when (iconStyle) {
            IconStyle.Filled -> R.drawable.stripe_ic_photo_camera
            IconStyle.Outlined -> R.drawable.stripe_ic_photo_camera_outlined
        }

        Image(
            painter = painterResource(icon),
            contentDescription = stringResource(
                R.string.stripe_scan_card
            ),
            colorFilter = ColorFilter.tint(MaterialTheme.colors.primary),
            modifier = Modifier
                .width(18.dp)
                .height(18.dp)
        )
        Text(
            stringResource(R.string.stripe_scan_card),
            Modifier
                .padding(start = 4.dp),
            color = MaterialTheme.colors.primary,
            style = MaterialTheme.typography.h6
        )
    }
}
