package com.stripe.android.paymentsheet.verticalmode

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R

@Composable
internal fun ChevronIcon(paymentMethodId: String?,) {
    Icon(
        painter = painterResource(R.drawable.stripe_ic_material_keyboard_arrow_right),
        contentDescription = null,
        tint = Color.Gray,
        modifier = Modifier.size(24.dp).semantics {
            this.testTag = "${TEST_TAG_MANAGE_SCREEN_CHEVRON_ICON}_$paymentMethodId"
        }
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val TEST_TAG_MANAGE_SCREEN_CHEVRON_ICON = "manage_screen_chevron_icon"
