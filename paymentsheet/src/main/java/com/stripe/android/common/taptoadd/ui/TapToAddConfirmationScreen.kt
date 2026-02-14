package com.stripe.android.common.taptoadd.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.uicore.strings.resolve

@Composable
internal fun TapToAddConfirmationScreen(
    state: TapToAddConfirmationInteractor.State,
) {
    TapToAddCardLayout {
        TapToAddCard(state.cardBrand, state.last4)

        Spacer(Modifier.size(20.dp))

        Text(
            text = state.title.resolve(),
            color = MaterialTheme.colors.onSurface,
            style = MaterialTheme.typography.h4.copy(
                fontWeight = FontWeight.Normal,
            ),
        )

        Spacer(Modifier.size(20.dp))

        with(state.primaryButton) {
            PrimaryButton(
                label = label.resolve(),
                locked = locked,
                enabled = true,
            ) {}
        }
    }
}
