package com.stripe.android.common.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import com.stripe.android.paymentsheet.R

@Composable
internal fun BottomSheetLoadingIndicator(
    modifier: Modifier = Modifier,
) {
    val height = dimensionResource(R.dimen.stripe_paymentsheet_loading_container_height)
    LoadingIndicator(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    )
}

@Composable
internal fun LoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.onSurface,
) {
    val indicatorSize = dimensionResource(R.dimen.stripe_paymentsheet_loading_indicator_size)
    val strokeWidth = dimensionResource(R.dimen.stripe_paymentsheet_loading_indicator_stroke_width)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        CircularProgressIndicator(
            color = color,
            strokeWidth = strokeWidth,
            modifier = Modifier.size(indicatorSize)
        )
    }
}
