package com.stripe.android.paymentsheet.model

import android.content.Context
import android.os.Parcelable
import androidx.annotation.DrawableRes
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.model.Address
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethod.Type.USBankAccount
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.paymentdatacollection.ach.ACHText
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormScreenState
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import com.stripe.android.ui.core.R as StripeUiCoreR

internal sealed class PaymentSelection : Parcelable {

    var hasAcknowledgedSepaMandate: Boolean = false

    abstract val requiresConfirmation: Boolean

    abstract fun mandateText(
        context: Context,
        merchantName: String,
        isSaveForFutureUseSelected: Boolean,
        isSetupFlow: Boolean,
    ): String?

    @Parcelize
    object GooglePay : PaymentSelection() {

        override val requiresConfirmation: Boolean
            get() = false

        override fun mandateText(
            context: Context,
            merchantName: String,
            isSaveForFutureUseSelected: Boolean,
            isSetupFlow: Boolean,
        ): String? {
            return null
        }
    }

    @Parcelize
    object Link : PaymentSelection() {

        override val requiresConfirmation: Boolean
            get() = false

        override fun mandateText(
            context: Context,
            merchantName: String,
            isSaveForFutureUseSelected: Boolean,
            isSetupFlow: Boolean,
        ): String? {
            return null
        }
    }

    @Parcelize
    data class Saved(
        val paymentMethod: PaymentMethod,
        val walletType: WalletType? = null,
    ) : PaymentSelection() {

        enum class WalletType(val paymentSelection: PaymentSelection) {
            GooglePay(PaymentSelection.GooglePay), Link(PaymentSelection.Link)
        }

        val showMandateAbovePrimaryButton: Boolean
            get() {
                return paymentMethod.type == PaymentMethod.Type.SepaDebit
            }

        override val requiresConfirmation: Boolean
            get() = paymentMethod.type == USBankAccount ||
                paymentMethod.type == PaymentMethod.Type.SepaDebit

        override fun mandateText(
            context: Context,
            merchantName: String,
            isSaveForFutureUseSelected: Boolean,
            isSetupFlow: Boolean,
        ): String? {
            return when (paymentMethod.type) {
                USBankAccount -> {
                    ACHText.getContinueMandateText(
                        context = context,
                        merchantName = merchantName,
                        isSaveForFutureUseSelected = isSaveForFutureUseSelected,
                        isSetupFlow = isSetupFlow,
                    )
                }
                PaymentMethod.Type.SepaDebit -> {
                    context.getString(StripeUiCoreR.string.stripe_sepa_mandate, merchantName)
                }
                else -> {
                    null
                }
            }
        }
    }

    enum class CustomerRequestedSave(val setupFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?) {
        RequestReuse(ConfirmPaymentIntentParams.SetupFutureUsage.OffSession),
        RequestNoReuse(ConfirmPaymentIntentParams.SetupFutureUsage.Blank),
        NoRequest(null)
    }

    sealed class New : PaymentSelection() {

        abstract val paymentMethodCreateParams: PaymentMethodCreateParams
        abstract val paymentMethodOptionsParams: PaymentMethodOptionsParams?
        abstract val paymentMethodExtraParams: PaymentMethodExtraParams?
        abstract val customerRequestedSave: CustomerRequestedSave

        override val requiresConfirmation: Boolean
            get() = false

        override fun mandateText(
            context: Context,
            merchantName: String,
            isSaveForFutureUseSelected: Boolean,
            isSetupFlow: Boolean,
        ): String? {
            return null
        }

        @Parcelize
        data class Card(
            override val paymentMethodCreateParams: PaymentMethodCreateParams,
            val brand: CardBrand,
            override val customerRequestedSave: CustomerRequestedSave,
            override val paymentMethodOptionsParams: PaymentMethodOptionsParams? = null,
            override val paymentMethodExtraParams: PaymentMethodExtraParams? = null,
        ) : New() {
            @IgnoredOnParcel
            val last4: String = paymentMethodCreateParams.cardLast4().orEmpty()
        }

        @Parcelize
        data class USBankAccount(
            val labelResource: String,
            @DrawableRes val iconResource: Int,
            val input: Input,
            val screenState: USBankAccountFormScreenState,
            override val paymentMethodCreateParams: PaymentMethodCreateParams,
            override val customerRequestedSave: CustomerRequestedSave,
            override val paymentMethodOptionsParams: PaymentMethodOptionsParams? = null,
            override val paymentMethodExtraParams: PaymentMethodExtraParams? = null,
        ) : New() {

            override fun mandateText(
                context: Context,
                merchantName: String,
                isSaveForFutureUseSelected: Boolean,
                isSetupFlow: Boolean,
            ): String? {
                return screenState.mandateText
            }

            @Parcelize
            data class Input(
                val name: String,
                val email: String?,
                val phone: String?,
                val address: Address?,
                val saveForFutureUse: Boolean,
            ) : Parcelable
        }

        @Parcelize
        data class LinkInline(val linkPaymentDetails: LinkPaymentDetails) : New() {
            @IgnoredOnParcel
            override val customerRequestedSave = CustomerRequestedSave.NoRequest

            @IgnoredOnParcel
            private val paymentDetails = linkPaymentDetails.paymentDetails

            @IgnoredOnParcel
            override val paymentMethodCreateParams = linkPaymentDetails.paymentMethodCreateParams

            @IgnoredOnParcel
            override val paymentMethodOptionsParams = null

            @IgnoredOnParcel
            override val paymentMethodExtraParams = null

            @IgnoredOnParcel
            @DrawableRes
            val iconResource = R.drawable.stripe_ic_paymentsheet_link

            @IgnoredOnParcel
            val label = when (paymentDetails) {
                is ConsumerPaymentDetails.Card ->
                    "····${paymentDetails.last4}"

                is ConsumerPaymentDetails.BankAccount ->
                    "····${paymentDetails.last4}"

                is ConsumerPaymentDetails.Passthrough ->
                    "····${paymentDetails.last4}"
            }
        }

        @Parcelize
        data class GenericPaymentMethod(
            val labelResource: String,
            @DrawableRes val iconResource: Int,
            val lightThemeIconUrl: String?,
            val darkThemeIconUrl: String?,
            override val paymentMethodCreateParams: PaymentMethodCreateParams,
            override val customerRequestedSave: CustomerRequestedSave,
            override val paymentMethodOptionsParams: PaymentMethodOptionsParams? = null,
            override val paymentMethodExtraParams: PaymentMethodExtraParams? = null,
        ) : New()
    }
}
