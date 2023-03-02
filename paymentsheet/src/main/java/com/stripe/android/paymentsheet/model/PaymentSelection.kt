package com.stripe.android.paymentsheet.model

import android.content.Context
import android.os.Parcelable
import androidx.annotation.DrawableRes
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethod.Type.USBankAccount
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.paymentdatacollection.ach.ACHText
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

internal sealed class PaymentSelection : Parcelable {

    abstract val requiresConfirmation: Boolean
    abstract fun mandateText(context: Context): String?

    @Parcelize
    object GooglePay : PaymentSelection() {

        override val requiresConfirmation: Boolean
            get() = false

        override fun mandateText(context: Context): String? {
            return null
        }
    }

    @Parcelize
    object Link : PaymentSelection() {

        override val requiresConfirmation: Boolean
            get() = false

        override fun mandateText(context: Context): String? {
            return null
        }
    }

    @Parcelize
    data class Saved(
        val paymentMethod: PaymentMethod,
        internal val isGooglePay: Boolean = false
    ) : PaymentSelection() {

        override val requiresConfirmation: Boolean
            get() = paymentMethod.type == USBankAccount

        override fun mandateText(context: Context): String? {
            return if (paymentMethod.type == USBankAccount) {
                ACHText.getContinueMandateText(context)
            } else {
                null
            }
        }
    }

    enum class CustomerRequestedSave {
        RequestReuse,
        RequestNoReuse,
        NoRequest
    }

    sealed class New : PaymentSelection() {

        abstract val paymentMethodCreateParams: PaymentMethodCreateParams
        abstract val customerRequestedSave: CustomerRequestedSave

        override val requiresConfirmation: Boolean
            get() = false

        override fun mandateText(context: Context): String? {
            return null
        }

        @Parcelize
        data class Card(
            override val paymentMethodCreateParams: PaymentMethodCreateParams,
            val brand: CardBrand,
            override val customerRequestedSave: CustomerRequestedSave
        ) : New() {
            @IgnoredOnParcel
            val last4: String = (
                (paymentMethodCreateParams.toParamMap()["card"] as? Map<*, *>)!!
                ["number"] as String
                )
                .takeLast(4)
        }

        @Parcelize
        data class USBankAccount(
            val labelResource: String,
            @DrawableRes val iconResource: Int,
            val bankName: String,
            val last4: String,
            val financialConnectionsSessionId: String,
            val intentId: String,
            override val paymentMethodCreateParams: PaymentMethodCreateParams,
            override val customerRequestedSave: CustomerRequestedSave
        ) : New()

        @Parcelize
        data class LinkInline(val linkPaymentDetails: LinkPaymentDetails.New) : New() {
            @IgnoredOnParcel
            override val customerRequestedSave = CustomerRequestedSave.NoRequest

            @IgnoredOnParcel
            private val paymentDetails = linkPaymentDetails.paymentDetails

            @IgnoredOnParcel
            override val paymentMethodCreateParams = linkPaymentDetails.paymentMethodCreateParams

            @IgnoredOnParcel
            @DrawableRes
            val iconResource = R.drawable.stripe_ic_paymentsheet_link

            @IgnoredOnParcel
            val label = when (paymentDetails) {
                is ConsumerPaymentDetails.Card ->
                    "····${paymentDetails.last4}"
                is ConsumerPaymentDetails.BankAccount ->
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
            override val customerRequestedSave: CustomerRequestedSave
        ) : New()
    }
}
