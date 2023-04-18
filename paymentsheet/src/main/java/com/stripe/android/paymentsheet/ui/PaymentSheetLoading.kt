package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.stripe.android.paymentsheet.R

@Composable
internal fun PaymentSheetLoading(
    modifier: Modifier = Modifier,
) {
    val height = dimensionResource(R.dimen.stripe_paymentsheet_loading_container_height)
    val indicatorSize = dimensionResource(R.dimen.stripe_paymentsheet_loading_indicator_size)
    val strokeWidth = dimensionResource(R.dimen.stripe_paymentsheet_loading_indicator_stroke_width)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colors.onSurface,
            strokeWidth = strokeWidth,
            modifier = modifier.size(indicatorSize),
        )
    }
}
