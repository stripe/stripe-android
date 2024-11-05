package com.stripe.android.paymentsheet

import android.os.Parcelable
import com.stripe.android.CardBrandFilter
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import kotlinx.parcelize.Parcelize
import com.stripe.android.model.PaymentMethod as PaymentMethodModel

internal sealed interface PaymentConfirmationOption : Parcelable {
    @Parcelize
    data class GooglePay(
        val initializationMode: PaymentSheet.InitializationMode,
        val shippingDetails: AddressDetails?,
        val config: Config,
    ) : PaymentConfirmationOption {
        @Parcelize
        data class Config(
            val environment: PaymentSheet.GooglePayConfiguration.Environment?,
            val merchantName: String,
            val merchantCountryCode: String,
            val merchantCurrencyCode: String?,
            val customAmount: Long?,
            val customLabel: String?,
            val billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
            val cardBrandFilter: CardBrandFilter
        ) : Parcelable
    }

    @Parcelize
    data class ExternalPaymentMethod(
        val type: String,
        val billingDetails: PaymentMethodModel.BillingDetails?,
    ) : PaymentConfirmationOption

    @Parcelize
    data class BacsPaymentMethod(
        val initializationMode: PaymentSheet.InitializationMode,
        val shippingDetails: AddressDetails?,
        val createParams: PaymentMethodCreateParams,
        val optionsParams: PaymentMethodOptionsParams?,
        val appearance: PaymentSheet.Appearance,
    ) : PaymentConfirmationOption

    sealed interface PaymentMethod : PaymentConfirmationOption {
        val initializationMode: PaymentSheet.InitializationMode
        val shippingDetails: AddressDetails?

        @Parcelize
        data class Saved(
            override val initializationMode: PaymentSheet.InitializationMode,
            override val shippingDetails: AddressDetails?,
            val paymentMethod: PaymentMethodModel,
            val optionsParams: PaymentMethodOptionsParams?,
        ) : PaymentMethod

        @Parcelize
        data class New(
            override val initializationMode: PaymentSheet.InitializationMode,
            override val shippingDetails: AddressDetails?,
            val createParams: PaymentMethodCreateParams,
            val optionsParams: PaymentMethodOptionsParams?,
            val shouldSave: Boolean
        ) : PaymentMethod
    }
}
