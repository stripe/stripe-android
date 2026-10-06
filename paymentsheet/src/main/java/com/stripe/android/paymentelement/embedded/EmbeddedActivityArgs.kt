package com.stripe.android.paymentelement.embedded

import android.content.Intent
import android.os.Parcelable
import androidx.core.os.BundleCompat
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import com.stripe.android.paymentsheet.state.CustomerState
import kotlinx.parcelize.Parcelize

internal sealed interface EmbeddedActivityArgs : Parcelable {
    val appearance: PaymentSheet.Appearance

    @Parcelize
    data class LoadingPaymentOptions(
        override val appearance: PaymentSheet.Appearance,
        val customerState: CustomerState?,
        val linkAccountInfo: LinkAccountUpdate.Value,
    ) : EmbeddedActivityArgs

    sealed interface Ready : EmbeddedActivityArgs {
        val context: ReadyContext
        val initialSelection: PaymentSelection?
        val customerState: CustomerState?

        override val appearance: PaymentSheet.Appearance
            get() = context.configuration.appearance

        val launchMode: EmbeddedLaunchMode
            get() = when (this) {
                is Form -> EmbeddedLaunchMode.Form(selectedPaymentMethodCode)
                is Manage -> EmbeddedLaunchMode.Manage
                is PaymentOptions -> EmbeddedLaunchMode.PaymentOptions
            }

        val promotions: List<PaymentMethodMessagePromotion>

        @Parcelize
        data class Form(
            override val context: ReadyContext,
            val selectedPaymentMethodCode: PaymentMethodCode,
            override val initialSelection: PaymentSelection.New?,
            override val customerState: CustomerState?,
            val promotion: PaymentMethodMessagePromotion?,
        ) : Ready {
            init {
                require(initialSelection == null || initialSelection.paymentMethodType == selectedPaymentMethodCode) {
                    "Initial selection must match the selected payment method code."
                }
                require(promotion == null || promotion.paymentMethodType.lowercase() == selectedPaymentMethodCode) {
                    "Promotion must match the selected payment method code."
                }
            }

            override val promotions: List<PaymentMethodMessagePromotion>
                get() = listOfNotNull(promotion)
        }

        @Parcelize
        data class Manage(
            override val context: ReadyContext,
            override val initialSelection: PaymentSelection?,
            override val customerState: CustomerState,
        ) : Ready {
            override val promotions: List<PaymentMethodMessagePromotion>
                get() = emptyList()
        }

        @Parcelize
        data class PaymentOptions(
            override val context: ReadyContext,
            override val initialSelection: PaymentSelection?,
            override val customerState: CustomerState?,
            override val promotions: List<PaymentMethodMessagePromotion>,
        ) : Ready
    }

    @Parcelize
    data class ReadyContext(
        val paymentMethodMetadata: PaymentMethodMetadata,
        val configuration: EmbeddedPaymentElement.Configuration,
        val productUsage: Set<String>,
        val paymentElementCallbackIdentifier: String,
        val statusBarColor: Int?,
        val linkAccountInfo: LinkAccountUpdate.Value,
        val previousNewSelections: PreviousNewSelections,
    ) : Parcelable

    companion object {
        internal const val EXTRA_ARGS: String = "extra_activity_args"

        fun fromIntent(intent: Intent): EmbeddedActivityArgs? {
            return intent.extras?.let { bundle ->
                BundleCompat.getParcelable(bundle, EXTRA_ARGS, EmbeddedActivityArgs::class.java)
            }
        }
    }
}
