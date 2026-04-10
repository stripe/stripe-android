package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.core.utils.FeatureFlags

internal object UIConstants {

    val iconWidth: Dp
        get() {
            return if (FeatureFlags.enableCardArt.isEnabled) {
                30.dp
            } else {
                24.dp
            }
        }

    val iconHeight: Dp
        get() {
            return if (FeatureFlags.enableCardArt.isEnabled) {
                25.dp
            } else {
                20.dp
            }
        }
}
