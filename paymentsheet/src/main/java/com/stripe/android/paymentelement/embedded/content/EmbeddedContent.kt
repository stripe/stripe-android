package com.stripe.android.paymentelement.embedded.content

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodEmbeddedLayoutUI
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor
import com.stripe.android.uicore.StripeTheme

@Immutable
@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal data class EmbeddedContent(
    private val interactor: PaymentMethodVerticalLayoutInteractor,
    private val embeddedViewDisplaysMandateText: Boolean,
    private val rowStyle: Embedded.RowStyle
) {
    @Composable
    fun Content() {
        StripeTheme {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colors.background)
                    .padding(top = 8.dp)
                    .animateContentSize()
            ) {
                PaymentMethodEmbeddedLayoutUI(
                    interactor = interactor,
                    embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText,
                    modifier = Modifier.padding(bottom = 8.dp),
                    rowStyle = rowStyle
                )
            }
        }
    }
}
