package com.stripe.android.paymentsheet.repositories

import com.stripe.android.core.model.StripeModel
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class CheckoutSessionResponse(
    val id: String,
    val amount: Long,
    val currency: String,
    val mode: Mode,
    val status: Status,
    val liveMode: Boolean,
    val taxStatus: TaxStatus,
    val customerEmail: String?,
    val elementsSession: ElementsSession?,
    val paymentIntent: PaymentIntent?,
    val setupIntent: SetupIntent?,
    val customer: Customer?,
    val savedPaymentMethodsOfferSave: SavedPaymentMethodsOfferSave?,
    val totalSummary: TotalSummaryResponse?,
    val lineItems: List<LineItem>,
    val shippingOptions: List<ShippingRate>,
    val adaptivePricingInfo: AdaptivePricingInfo?,
    val automaticTaxEnabled: Boolean,
    val automaticTaxAddressSource: String?,
) : StripeModel {

    /**
     * Returns true if the tax region should be sent to the server for the given address type.
     * Only sends when automatic tax is enabled and the address source matches the address type.
     */
    fun shouldSendTaxRegion(addressType: String): Boolean {
        return automaticTaxEnabled && automaticTaxAddressSource == addressType
    }

    @Parcelize
    data class SavedPaymentMethodsOfferSave(
        val enabled: Boolean,
        val status: Status,
    ) : StripeModel {
        enum class Status {
            ACCEPTED,
            NOT_ACCEPTED,
        }
    }

    @Parcelize
    data class Customer(
        val id: String,
        val paymentMethods: List<PaymentMethod>,
        val canDetachPaymentMethod: Boolean,
    ) : StripeModel

    @Parcelize
    data class TotalSummaryResponse(
        val subtotal: Long,
        val totalDueToday: Long,
        val totalAmountDue: Long,
        val discountAmounts: List<DiscountAmount>,
        val taxAmounts: List<TaxAmount>,
        val shippingRate: ShippingRate?,
        val appliedBalance: Long?,
    ) : StripeModel

    @Parcelize
    data class DiscountAmount(
        val amount: Long,
        val displayName: String,
    ) : StripeModel

    @Parcelize
    data class TaxAmount(
        val amount: Long,
        val inclusive: Boolean,
        val displayName: String,
        val percentage: Double,
    ) : StripeModel

    @Parcelize
    data class ShippingRate(
        val id: String,
        val amount: Long,
        val displayName: String,
        val deliveryEstimate: String?,
    ) : StripeModel

    @Parcelize
    data class LineItem(
        val id: String,
        val name: String,
        val quantity: Int,
        val unitAmount: Long?,
        val subtotal: Long,
        val total: Long,
    ) : StripeModel

    @Parcelize
    data class AdaptivePricingInfo(
        val activePresentmentCurrency: String,
        val integrationAmount: Long,
        val integrationCurrency: String,
        val localCurrencyOptions: List<LocalCurrencyOption>,
    ) : StripeModel

    @Parcelize
    data class LocalCurrencyOption(
        val amount: Long,
        val conversionMarkupBps: Int,
        val currency: String,
        val presentmentExchangeRate: String,
    ) : StripeModel

    enum class Mode {
        PAYMENT,
        SETUP,
        UNKNOWN,
    }

    enum class Status {
        OPEN,
        COMPLETE,
        EXPIRED,
        UNKNOWN,
    }

    enum class TaxStatus {
        READY,
        REQUIRES_SHIPPING_ADDRESS,
        REQUIRES_BILLING_ADDRESS,
        UNKNOWN,
    }
}
