package com.stripe.android.paymentelement.embedded

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.ErrorMessage
import com.stripe.android.paymentsheet.ui.Mandate
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutUI
import com.stripe.android.uicore.strings.resolve

@Immutable
internal data class EmbeddedContent(
    private val interactor: PaymentMethodVerticalLayoutInteractor,
    private val error: ResolvableString? = null,
    private val mandate: ResolvableString? = null,
) {
    @Composable
    fun Content() {
        val horizontalPadding = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal)
        Column(
            modifier = Modifier
                .padding(horizontal = horizontalPadding)
                .padding(top = 8.dp)
        ) {
            EmbeddedVerticalList()
            EmbeddedError()
            EmbeddedMandate()
        }
    }

    @Composable
    private fun EmbeddedVerticalList() {
        PaymentMethodVerticalLayoutUI(
            interactor = interactor,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }

    @Composable
    private fun EmbeddedError() {
        error?.let {
            ErrorMessage(
                error = it.resolve(),
                modifier = Modifier
                    .padding(top = 2.dp, bottom = 8.dp)
                    .testTag(EMBEDDED_ERROR_TEXT_TEST_TAG),
            )
        }
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
        const val EMBEDDED_ERROR_TEXT_TEST_TAG = "EMBEDDED_ERROR"
        const val EMBEDDED_MANDATE_TEXT_TEST_TAG = "EMBEDDED_MANDATE"
    }
}
