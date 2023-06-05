package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.shouldUseDarkDynamicColor

@Composable
internal fun Loading() {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val isDark = MaterialTheme.colors.surface.shouldUseDarkDynamicColor()
        CircularProgressIndicator(
            modifier = Modifier.size(
                dimensionResource(R.dimen.stripe_paymentsheet_loading_indicator_size)
            ),
            color = if (isDark) Color.Black else Color.White,
            strokeWidth = dimensionResource(
                R.dimen.stripe_paymentsheet_loading_indicator_stroke_width
            )
        )
    }
}
