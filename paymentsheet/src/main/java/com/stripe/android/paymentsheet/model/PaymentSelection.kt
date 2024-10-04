package com.stripe.android.paymentsheet.model

import android.os.Parcelable
import androidx.annotation.DrawableRes
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.model.Address
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethod.Type.USBankAccount
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.paymentdatacollection.ach.BankFormScreenState
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountTextBuilder
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import com.stripe.android.ui.core.R as StripeUiCoreR

internal sealed class PaymentSelection : Parcelable {

    var hasAcknowledgedSepaMandate: Boolean = false

    abstract val requiresConfirmation: Boolean

    abstract fun mandateText(
        merchantName: String,
        isSetupFlow: Boolean,
    ): ResolvableString?

    @Parcelize
    data object GooglePay : PaymentSelection() {

        override val requiresConfirmation: Boolean
            get() = false

        override fun mandateText(
            merchantName: String,
            isSetupFlow: Boolean,
        ): ResolvableString? {
            return null
        }
    }

    @Parcelize
    data object Link : PaymentSelection() {

        override val requiresConfirmation: Boolean
            get() = false

        override fun mandateText(
            merchantName: String,
            isSetupFlow: Boolean,
        ): ResolvableString? {
            return null
        }
    }

    @Parcelize
    data class ExternalPaymentMethod(
        val type: String,
        val billingDetails: PaymentMethod.BillingDetails?,
        val label: ResolvableString,
        // In practice, we don't have an iconResource for external payment methods.
        @DrawableRes val iconResource: Int,
        // In practice, we always have a lightThemeIconUrl for external payment methods.
        val lightThemeIconUrl: String?,
        val darkThemeIconUrl: String?,
    ) : PaymentSelection() {
        override val requiresConfirmation: Boolean
            get() = false

        override fun mandateText(
            merchantName: String,
            isSetupFlow: Boolean
        ): ResolvableString? {
            return null
        }
    }

    @Parcelize
    data class Saved(
        val paymentMethod: PaymentMethod,
        val walletType: WalletType? = null,
        val paymentMethodOptionsParams: PaymentMethodOptionsParams? = null,
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
            merchantName: String,
            isSetupFlow: Boolean,
        ): ResolvableString? {
            return when (paymentMethod.type) {
                USBankAccount -> {
                    USBankAccountTextBuilder.buildMandateAndMicrodepositsText(
                        merchantName = merchantName,
                        isVerifyingMicrodeposits = false,
                        isSaveForFutureUseSelected = false,
                        isInstantDebits = false,
                        isSetupFlow = isSetupFlow,
                    )
                }
                PaymentMethod.Type.SepaDebit -> {
                    resolvableString(StripeUiCoreR.string.stripe_sepa_mandate, merchantName)
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
            merchantName: String,
            isSetupFlow: Boolean,
        ): ResolvableString? {
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
            val screenState: BankFormScreenState,
            val instantDebits: InstantDebitsInfo?,
            override val paymentMethodCreateParams: PaymentMethodCreateParams,
            override val customerRequestedSave: CustomerRequestedSave,
            override val paymentMethodOptionsParams: PaymentMethodOptionsParams? = null,
            override val paymentMethodExtraParams: PaymentMethodExtraParams? = null,
        ) : New() {

            override fun mandateText(
                merchantName: String,
                isSetupFlow: Boolean,
            ): ResolvableString? {
                return screenState.linkedBankAccount?.mandateText
            }

            @Parcelize
            data class InstantDebitsInfo(
                val paymentMethod: PaymentMethod,
                val linkMode: LinkMode?,
                val incentiveEligible: Boolean = false,
            ) : Parcelable

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
        data class LinkInline(
            val linkPaymentDetails: LinkPaymentDetails,
            override val customerRequestedSave: CustomerRequestedSave,
        ) : New() {
            @IgnoredOnParcel
            private val paymentDetails = linkPaymentDetails.paymentDetails

            @IgnoredOnParcel
            override val paymentMethodCreateParams = linkPaymentDetails.paymentMethodCreateParams

            @IgnoredOnParcel
            override val paymentMethodOptionsParams = PaymentMethodOptionsParams.Card(
                setupFutureUsage = customerRequestedSave.setupFutureUsage
            )

            @IgnoredOnParcel
            override val paymentMethodExtraParams = null

            @IgnoredOnParcel
            @DrawableRes
            val iconResource = R.drawable.stripe_ic_paymentsheet_link

            @IgnoredOnParcel
            val label = "···· ${paymentDetails.last4}"
        }

        @Parcelize
        data class GenericPaymentMethod(
            val label: ResolvableString,
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

internal val PaymentSelection.isLink: Boolean
    get() = when (this) {
        is PaymentSelection.GooglePay -> false
        is PaymentSelection.Link -> true
        is PaymentSelection.New.LinkInline -> true
        is PaymentSelection.New -> false
        is PaymentSelection.Saved -> walletType == PaymentSelection.Saved.WalletType.Link
        is PaymentSelection.ExternalPaymentMethod -> false
    }

internal val PaymentSelection.isSaved: Boolean
    get() = when (this) {
        is PaymentSelection.Saved -> true
        else -> false
    }
