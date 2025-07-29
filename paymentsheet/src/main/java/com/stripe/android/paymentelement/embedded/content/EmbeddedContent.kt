package com.stripe.android.paymentelement.embedded.content

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.stripe.android.elements.Appearance.Embedded
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodEmbeddedLayoutUI
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor
import com.stripe.android.uicore.StripeTheme

@Immutable
internal data class EmbeddedContent(
    private val interactor: PaymentMethodVerticalLayoutInteractor,
    private val embeddedViewDisplaysMandateText: Boolean,
    private val appearance: Embedded,
    private val isImmediateAction: Boolean,
) {
    @Composable
    fun Content() {
        /**
         * This validation is here because of a weird interaction with 2-Step Integration.
         *
         * If this were in configure or when state is set, then it would fail for the 1st instance of embedded
         * in the 2 step integration because a user would not set a rowSelectionBehavior on the 1st instance of embedded
         * because the 1st instance doesn't show a UI. However, the first instance still has to be configured so it will
         * fail unless the user sets an empty rowSelection ImmediateAction callback.
         *
         * Having validation here ensures that we only validate when the embedded content is shown.
         */
        LaunchedEffect(appearance.style, isImmediateAction) {
            if (appearance.style is Embedded.RowStyle.FlatWithDisclosure && !isImmediateAction) {
                throw IllegalArgumentException(
                    "EmbeddedPaymentElement.Builder.rowSelectionBehavior() must be set to ImmediateAction when using " +
                        "FlatWithDisclosure RowStyle. Use a different style or enable ImmediateAction " +
                        "rowSelectionBehavior"
                )
            }
        }

        StripeTheme {
            Column(
                modifier = Modifier
                    .animateContentSize()
            ) {
                PaymentMethodEmbeddedLayoutUI(
                    interactor = interactor,
                    embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText,
                    appearance = appearance
                )
            }
        }
    }
}
