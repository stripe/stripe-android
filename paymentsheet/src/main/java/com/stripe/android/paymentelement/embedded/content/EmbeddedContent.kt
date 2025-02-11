package com.stripe.android.paymentelement.embedded.content

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.ui.Mandate
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodEmbeddedLayoutUI
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.strings.resolve

@Immutable
@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal data class EmbeddedContent(
    private val interactor: PaymentMethodVerticalLayoutInteractor,
    val mandate: ResolvableString? = null,
    private val rowStyle: Embedded.RowStyle
) {
    @Composable
    fun Content() {
        StripeTheme {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colors.surface)
                    .padding(top = 8.dp)
                    .animateContentSize()
            ) {
                EmbeddedVerticalList()
                EmbeddedMandate()
            }
        }
    }

    @Composable
    private fun EmbeddedVerticalList() {
        PaymentMethodEmbeddedLayoutUI(
            interactor = interactor,
            modifier = Modifier.padding(bottom = 8.dp),
            rowStyle = rowStyle
        )
    }

    @Composable
    private fun EmbeddedMandate() {
        Mandate(
            mandateText = mandate?.resolve(),
            modifier = Modifier
                .padding(bottom = 8.dp)
                .testTag(EMBEDDED_MANDATE_TEXT_TEST_TAG),
        )
    }

    companion object {
        const val EMBEDDED_MANDATE_TEXT_TEST_TAG = "EMBEDDED_MANDATE"
    }
}
