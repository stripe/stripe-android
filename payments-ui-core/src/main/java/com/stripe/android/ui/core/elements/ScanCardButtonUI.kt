package com.stripe.android.ui.core.elements

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.cardscan.CardScanActivity

@Composable
internal fun ScanCardButtonUI(
    onResult: (intent: Intent) -> Unit
) {
    val context = LocalContext.current

    val cardScanLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            it.data?.let {
                onResult(it)
            }
        }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
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
            painter = painterResource(R.drawable.ic_photo_camera),
            contentDescription = stringResource(
                R.string.scan_card
            ),
            colorFilter = ColorFilter.tint(PaymentsTheme.colors.material.primary),
            modifier = Modifier
                .width(18.dp)
                .height(18.dp)
        )
        Text(
            stringResource(R.string.scan_card),
            Modifier
                .padding(start = 4.dp),
            color = PaymentsTheme.colors.material.primary,
            style = PaymentsTheme.typography.h6,
        )
    }
}
