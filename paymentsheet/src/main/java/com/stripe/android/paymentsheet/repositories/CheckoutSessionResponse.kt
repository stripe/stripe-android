package com.stripe.android.paymentsheet.repositories

import com.stripe.android.core.StripeError
import com.stripe.android.core.model.StripeModel
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class CheckoutSessionResponse(
    val id: String,
    val clientSecret: String,
    val configId: String?,
    val currency: String?,
    val integrationCurrency: String?,
    val paymentStatus: PaymentStatus,
    val status: Status,
    val livemode: Boolean,
    val customerEmail: String?,
    val elementsSession: ElementsSession?,
    val paymentIntent: PaymentIntent?,
    val setupIntent: SetupIntent?,
    val customer: Customer?,
    val savedPaymentMethodsOfferSave: SavedPaymentMethodsOfferSave?,
    val checkoutItems: List<CheckoutItem>,
    val totalSummary: TotalSummary?,
    val adaptivePricingInfo: AdaptivePricingInfo?,
    val taxMeta: TaxMeta?,
    val automaticTaxEnabled: Boolean,
    val taxAddressSource: TaxAddressSource?,
    val allowedShippingCountries: List<String>?,
    val requiresShippingAddress: Boolean,
    val requiresBillingAddress: Boolean,
    val merchantCountry: String?,
    val businessName: String?,
    val paymentResponse: PaymentResponse?,
) : StripeModel {
    @Parcelize
    data class IntentReference(val id: String, val clientSecret: String) : StripeModel

    @Parcelize
    data class PaymentResponse(
        val status: String,
        val paymentIntent: IntentReference?,
        val setupIntent: IntentReference?,
        val error: StripeError?,
    ) : StripeModel

    /** Reconcile the session with an Intent whose authentication has completed on this device. */
    fun withConfirmedIntent(intent: StripeIntent): CheckoutSessionResponse {
        val complete = when (intent) {
            is PaymentIntent ->
                intent.status == StripeIntent.Status.Succeeded || intent.status == StripeIntent.Status.Processing
            is SetupIntent -> intent.status == StripeIntent.Status.Succeeded
        }
        return copy(
            status = if (complete) Status.COMPLETE else status,
            paymentStatus = if (intent is PaymentIntent && intent.status == StripeIntent.Status.Succeeded) {
                PaymentStatus.PAID
            } else {
                paymentStatus
            },
            paymentIntent = intent as? PaymentIntent,
            setupIntent = intent as? SetupIntent,
            elementsSession = elementsSession?.copy(stripeIntent = intent),
        )
    }

    val fixedEmail: String?
        get() = customerEmail ?: customer?.email

    val amount: Long
        get() = checkoutItems.sumOf { group -> group.oneTimePrice.items.sumOf { it.total } }

    val collectsTaxFromBillingAddress: Boolean
        get() = automaticTaxEnabled && taxAddressSource == TaxAddressSource.BILLING

    val noPaymentRequired: Boolean
        get() = paymentStatus == PaymentStatus.NO_PAYMENT_REQUIRED

    enum class TaxAddressSource { SHIPPING, BILLING }
    enum class PaymentStatus { PAID, UNPAID, NO_PAYMENT_REQUIRED }
    enum class Status { OPEN, COMPLETE, EXPIRED }

    @Parcelize
    data class SavedPaymentMethodsOfferSave(val enabled: Boolean) : StripeModel

    @Parcelize
    data class Customer(
        val id: String,
        val email: String?,
        val paymentMethods: List<PaymentMethod>,
        val canDetachPaymentMethod: Boolean,
    ) : StripeModel

    @Parcelize
    data class CheckoutItem(val key: String, val oneTimePrice: OneTimePrice) : StripeModel

    @Parcelize
    data class OneTimePrice(val items: List<OneTimePriceItem>) : StripeModel

    @Parcelize
    data class OneTimePriceItem(
        val innerItemKey: String,
        val price: Price,
        val quantity: Int,
        val subtotal: Long,
        val total: Long,
        val unitAmount: Long?,
        val unitAmountDecimal: Double?,
        val unitLabel: String?,
        val taxAmounts: List<TaxAmount>,
        val taxInclusive: Long,
        val taxExclusive: Long,
        val adjustableQuantity: AdjustableQuantity?,
    ) : StripeModel

    @Parcelize
    data class Price(
        val id: String,
        val currency: String,
        val unitAmount: Long?,
        val product: Product,
    ) : StripeModel

    @Parcelize
    data class Product(val name: String, val images: List<String>) : StripeModel

    @Parcelize
    data class AdjustableQuantity(
        val enabled: Boolean,
        val maximum: Int?,
        val minimum: Int?,
    ) : StripeModel

    @Parcelize
    data class TotalSummary(val totalTaxAmounts: List<TaxAmount>) : StripeModel

    @Parcelize
    data class TaxAmount(val amount: Long, val inclusive: Boolean, val taxRate: TaxRate) : StripeModel

    @Parcelize
    data class TaxRate(
        val displayName: String,
        val percentage: Double,
        val rateType: TaxRateType?,
    ) : StripeModel

    enum class TaxRateType { FLAT_AMOUNT, PERCENTAGE }

    @Parcelize
    data class TaxMeta(val computationType: TaxComputationType, val status: TaxStatus?) : StripeModel

    enum class TaxComputationType { OFF, AUTOMATIC, EXTENSION_DEFINED, MANUAL, USER_DEFINED }
    enum class TaxStatus { COMPLETE, FAILED, REQUIRES_LOCATION_INPUTS }

    @Parcelize
    data class AdaptivePricingInfo(
        val activePresentmentCurrency: String?,
        val integrationAmount: Long,
        val integrationCurrency: String,
        val integrationDisclosureCaption: String?,
        val localCurrencyOptions: List<LocalCurrencyOption>,
    ) : StripeModel {
        val isActive: Boolean
            get() = activePresentmentCurrency != null &&
                !activePresentmentCurrency.equals(integrationCurrency, ignoreCase = true)
    }

    @Parcelize
    data class LocalCurrencyOption(
        val amount: Long,
        val currency: String,
        val disclosureCaption: String?,
        val disclosureDetails: String?,
    ) : StripeModel
}

internal fun CheckoutSessionResponse.validateShippingCountry(country: String): Result<Unit> {
    val allowed = allowedShippingCountries ?: return Result.success(Unit)
    return if (country in allowed) {
        Result.success(Unit)
    } else {
        Result.failure(IllegalArgumentException("Country code '$country' is not in allowedShippingCountries"))
    }
}
