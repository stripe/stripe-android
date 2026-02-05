package com.stripe.android.common.taptoadd

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R

@Composable
internal fun TapToButtonUI(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            enabled = enabled,
            onClick = onClick,
        ),
    ) {
        Image(
            painter = painterResource(R.drawable.stripe_ic_nfc_tap),
            contentDescription = stringResource(R.string.stripe_tap_to_add_card_button_label),
            colorFilter = ColorFilter.tint(MaterialTheme.colors.primary),
            modifier = Modifier.width(18.dp).height(18.dp),
        )
        Text(
            text = stringResource(R.string.stripe_tap_to_add_card_button_label),
            modifier = Modifier.padding(start = 4.dp),
            color = MaterialTheme.colors.primary,
            style = MaterialTheme.typography.h6,
        )
    }
}
