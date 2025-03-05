package com.stripe.android.paymentsheet.verticalmode

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp

@Composable
internal fun ChevronIcon(paymentMethodId: String?,) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = Color.Gray,
        modifier = Modifier.size(24.dp).semantics {
            this.testTag = "${TEST_TAG_MANAGE_SCREEN_CHEVRON_ICON}_$paymentMethodId"
        }
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val TEST_TAG_MANAGE_SCREEN_CHEVRON_ICON = "manage_screen_chevron_icon"
