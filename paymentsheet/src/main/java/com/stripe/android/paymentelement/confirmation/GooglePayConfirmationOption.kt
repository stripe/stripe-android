package com.stripe.android.paymentelement.confirmation

import android.os.Parcelable
import com.stripe.android.CardBrandFilter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class GooglePayConfirmationOption(
    val initializationMode: PaymentElementLoader.InitializationMode,
    val shippingDetails: AddressDetails?,
    val config: Config,
) : ConfirmationHandler.Option {
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
