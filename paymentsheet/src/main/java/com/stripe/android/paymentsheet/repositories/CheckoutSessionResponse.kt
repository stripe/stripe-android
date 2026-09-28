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
    val currency: String,
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
    val recurringDetails: RecurringDetails?,
    val adaptivePricingInfo: AdaptivePricingInfo?,
    val taxMeta: TaxMeta?,
    val automaticTaxEnabled: Boolean,
    val taxAddressSource: TaxAddressSource?,
    val allowedShippingCountries: List<String>?,
    val requiresShippingAddress: Boolean,
    val requiresBillingAddress: Boolean,
    val merchantCountry: String?,
    val businessName: String?,
) : StripeModel {
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
    data class SavedPaymentMethodsOfferSave(val enabled: Boolean, val status: Status) : StripeModel {
        enum class Status { ACCEPTED, NOT_ACCEPTED }
    }

    @Parcelize
    data class Customer(
        val id: String,
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
    data class RecurringDetails(
        val totalDiscountAmounts: List<DiscountAmount>,
        val totalTaxAmounts: List<TaxAmount>,
    ) : StripeModel

    @Parcelize
    data class DiscountAmount(
        val amount: Long,
        val displayName: String?,
        val coupon: Coupon,
        val promotionCode: PromotionCode?,
    ) : StripeModel

    @Parcelize
    data class Coupon(val code: String, val name: String?, val percentOff: Double?) : StripeModel

    @Parcelize
    data class PromotionCode(val code: String?) : StripeModel

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
        val activePresentmentCurrency: String,
        val integrationAmount: Long,
        val integrationCurrency: String,
        val localCurrencyOptions: List<LocalCurrencyOption>,
    ) : StripeModel

    @Parcelize
    data class LocalCurrencyOption(
        val amount: Long,
        val conversionMarkupBps: Int?,
        val currency: String,
        val presentmentExchangeRate: String,
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
