package com.stripe.android.paymentsheet.utils

import android.view.View
import android.view.WindowInsets
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.core.view.doOnAttach
import com.stripe.android.paymentsheet.R
import androidx.compose.foundation.layout.WindowInsets.Companion as ComposeWindowInsets

// Taken from https://medium.com/androiddevelopers/windowinsets-listeners-to-layouts-8f9ccc8fa4d1
internal fun View.doOnApplyWindowInsets(onApplyInsets: (View, WindowInsets, InitialPadding) -> Unit) {
    val initialPadding = InitialPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
    setOnApplyWindowInsetsListener { v, insets ->
        onApplyInsets(v, insets, initialPadding)
        insets
    }
    requestApplyInsetsWhenAttached()
}

internal data class InitialPadding(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

internal fun View.requestApplyInsetsWhenAttached() {
    doOnAttach {
        it.requestApplyInsets()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PaymentSheetContentPadding() {
    val bottomPadding = dimensionResource(R.dimen.stripe_paymentsheet_button_container_spacing_bottom)
    Spacer(modifier = Modifier.requiredHeight(bottomPadding))
    Spacer(modifier = Modifier.windowInsetsBottomHeight(ComposeWindowInsets.navigationBars))
}
