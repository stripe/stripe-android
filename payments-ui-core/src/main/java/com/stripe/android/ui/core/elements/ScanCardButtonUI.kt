package com.stripe.android.ui.core.elements

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.cardscan.CardScanActivity
import com.stripe.android.uicore.analytics.rememberInteractionReporter

@Composable
internal fun ScanCardButtonUI(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onResult: (intent: Intent) -> Unit,
) {
    val context = LocalContext.current
    val reportingElements = rememberInteractionReporter()

    val cardScanLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            it.data?.let {
                onResult(it)
            }
        }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clickable(
            interactionSource = reportingElements.interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = {
                cardScanLauncher.launch(
                    Intent(
                        context,
                        CardScanActivity::class.java
                    )
                )
            }
        )
    ) {
        Image(
            painter = painterResource(R.drawable.stripe_ic_photo_camera),
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
