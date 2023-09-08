package com.stripe.android.paymentsheet.forms

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
sealed interface Requirement

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
sealed interface PIRequirement : Requirement

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
sealed interface SIRequirement : Requirement

/**
 * This requirement is dependent on the configuration passed by the app to the SDK.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object Delayed : PIRequirement, SIRequirement

/**
 * The Payment Method requires a shipping address in the Payment Intent.
 * The fields required are name, address line 1, country, and postal code.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object ShippingAddress : PIRequirement

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class PaymentMethodRequirements(
    val paymentIntentSupport: PaymentIntentSupport,
    val setupIntentSupport: SetupIntentSupport,
    val canBeUsedAsSavedPaymentMethod: Boolean,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
sealed interface PaymentIntentSupport {
    object AlwaysSupported : PaymentIntentSupport

    data class SupportedWithRequirements(
        val requirements: Set<PIRequirement>,
    ) : PaymentIntentSupport {
        constructor(vararg requirements: PIRequirement) : this(requirements.toSet())
    }

    object NotSupported : PaymentIntentSupport
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
sealed interface SetupIntentSupport {
    object AlwaysSupported : SetupIntentSupport

    data class SupportedWithRequirements(
        val requirements: Set<SIRequirement>,
    ) : SetupIntentSupport {
        constructor(vararg requirements: SIRequirement) : this(requirements.toSet())
    }

    object NotSupported : SetupIntentSupport
}

internal val CardRequirement = PaymentMethodRequirements(
    paymentIntentSupport = PaymentIntentSupport.AlwaysSupported,
    setupIntentSupport = SetupIntentSupport.AlwaysSupported,
    canBeUsedAsSavedPaymentMethod = true,
)

internal val BancontactRequirement = PaymentMethodRequirements(
    paymentIntentSupport = PaymentIntentSupport.AlwaysSupported,
    setupIntentSupport = SetupIntentSupport.SupportedWithRequirements(Delayed),
    /**
     * PM will be attached as a SEPA Debit payment method and have the requirements
     * of that PaymentMethod.
     */
    canBeUsedAsSavedPaymentMethod = true,
)

internal val SofortRequirement = PaymentMethodRequirements(
    paymentIntentSupport = PaymentIntentSupport.SupportedWithRequirements(Delayed),
    setupIntentSupport = SetupIntentSupport.SupportedWithRequirements(Delayed),
    /**
     * PM will be attached as a SEPA Debit payment method and have the requirements
     * of that PaymentMethod.
     */
    canBeUsedAsSavedPaymentMethod = true,
)

internal val IdealRequirement = PaymentMethodRequirements(
    paymentIntentSupport = PaymentIntentSupport.AlwaysSupported,
    setupIntentSupport = SetupIntentSupport.SupportedWithRequirements(Delayed),
    /**
     * PM will be attached as a SEPA Debit payment method and have the requirements
     * of that PaymentMethod.
     */
    canBeUsedAsSavedPaymentMethod = true,
)

internal val SepaDebitRequirement = PaymentMethodRequirements(
    paymentIntentSupport = PaymentIntentSupport.SupportedWithRequirements(Delayed),
    setupIntentSupport = SetupIntentSupport.SupportedWithRequirements(Delayed),
    /**
     * Here we explain the details
     * - if PI w/SFU set or SI with a customer, or
     * - if PI w/SFU set or SI with/out a customer and later attached when used with a webhook
     * (Note: from the client there is no way to detect if a PI or SI is associated with a customer)
     *
     * then, this payment method would be attached to the customer as a SEPA payment method.
     * (Note: Bancontact, iDEAL, and Sofort require authentication, but SEPA does not.
     * also Bancontact, iDEAL are not delayed, but Sofort and SEPA are delayed.)
     *
     * The SEPA payment method requires a mandate when confirmed.
     */
    canBeUsedAsSavedPaymentMethod = true,
)

internal val EpsRequirement = PaymentMethodRequirements(
    paymentIntentSupport = PaymentIntentSupport.AlwaysSupported,
    setupIntentSupport = SetupIntentSupport.NotSupported,
    canBeUsedAsSavedPaymentMethod = false,
)

internal val P24Requirement = PaymentMethodRequirements(
    paymentIntentSupport = PaymentIntentSupport.AlwaysSupported,
    setupIntentSupport = SetupIntentSupport.NotSupported,
    canBeUsedAsSavedPaymentMethod = false,
)

internal val GiropayRequirement = PaymentMethodRequirements(
    paymentIntentSupport = PaymentIntentSupport.AlwaysSupported,
    setupIntentSupport = SetupIntentSupport.NotSupported,
    canBeUsedAsSavedPaymentMethod = false,
)

/**
 * This defines the requirements for usage as a Payment Method.
 */
internal val AfterpayClearpayRequirement = PaymentMethodRequirements(
    paymentIntentSupport = PaymentIntentSupport.SupportedWithRequirements(ShippingAddress),
    setupIntentSupport = SetupIntentSupport.NotSupported,
    canBeUsedAsSavedPaymentMethod = false,
)

/**
 * This defines the requirements for usage as a Payment Method.
 */
internal val KlarnaRequirement = PaymentMethodRequirements(
    paymentIntentSupport = PaymentIntentSupport.AlwaysSupported,
    setupIntentSupport = SetupIntentSupport.NotSupported,
    canBeUsedAsSavedPaymentMethod = false,
)

/**
 * This defines the requirements for usage as a Payment Method.
 */
internal val PaypalRequirement = PaymentMethodRequirements(
    paymentIntentSupport = PaymentIntentSupport.AlwaysSupported,
    setupIntentSupport = SetupIntentSupport.AlwaysSupported,
    canBeUsedAsSavedPaymentMethod = false,
)

/**
 * This defines the requirements for usage as a Payment Method.
 */
internal val AffirmRequirement = PaymentMethodRequirements(
    paymentIntentSupport = PaymentIntentSupport.SupportedWithRequirements(ShippingAddress),
    setupIntentSupport = SetupIntentSupport.NotSupported,
    canBeUsedAsSavedPaymentMethod = false,
)

internal val RevolutPayRequirement = PaymentMethodRequirements(
    paymentIntentSupport = PaymentIntentSupport.AlwaysSupported,
    setupIntentSupport = SetupIntentSupport.NotSupported,
    canBeUsedAsSavedPaymentMethod = false,
)

internal val AmazonPayRequirement = PaymentMethodRequirements(
    paymentIntentSupport = PaymentIntentSupport.AlwaysSupported,
    setupIntentSupport = SetupIntentSupport.NotSupported,
    canBeUsedAsSavedPaymentMethod = false,
)

internal val MobilePayRequirement = PaymentMethodRequirements(
    paymentIntentSupport = PaymentIntentSupport.AlwaysSupported,
    setupIntentSupport = SetupIntentSupport.NotSupported,
    canBeUsedAsSavedPaymentMethod = false,
)

/**
 * This defines the requirements for usage as a Payment Method.
 */
internal val AuBecsDebitRequirement = PaymentMethodRequirements(
    paymentIntentSupport = PaymentIntentSupport.SupportedWithRequirements(Delayed),
    setupIntentSupport = SetupIntentSupport.SupportedWithRequirements(Delayed),
    canBeUsedAsSavedPaymentMethod = false,
)

internal val ZipRequirement = PaymentMethodRequirements(
    paymentIntentSupport = PaymentIntentSupport.AlwaysSupported,
    setupIntentSupport = SetupIntentSupport.NotSupported,
    canBeUsedAsSavedPaymentMethod = false,
)

/**
 * This defines the requirements for usage as a Payment Method.
 */
internal val USBankAccountRequirement = PaymentMethodRequirements(
    paymentIntentSupport = PaymentIntentSupport.SupportedWithRequirements(Delayed),
    setupIntentSupport = SetupIntentSupport.SupportedWithRequirements(Delayed),
    canBeUsedAsSavedPaymentMethod = true,
)

internal val UpiRequirement = PaymentMethodRequirements(
    paymentIntentSupport = PaymentIntentSupport.AlwaysSupported,
    setupIntentSupport = SetupIntentSupport.NotSupported,
    canBeUsedAsSavedPaymentMethod = false,
)

internal val BlikRequirement = PaymentMethodRequirements(
    paymentIntentSupport = PaymentIntentSupport.AlwaysSupported,
    setupIntentSupport = SetupIntentSupport.NotSupported,
    canBeUsedAsSavedPaymentMethod = false,
)

internal val CashAppPayRequirement = PaymentMethodRequirements(
    paymentIntentSupport = PaymentIntentSupport.AlwaysSupported,
    setupIntentSupport = SetupIntentSupport.AlwaysSupported,
    canBeUsedAsSavedPaymentMethod = false,
)

internal val GrabPayRequirement = PaymentMethodRequirements(
    paymentIntentSupport = PaymentIntentSupport.AlwaysSupported,
    setupIntentSupport = SetupIntentSupport.NotSupported,
    canBeUsedAsSavedPaymentMethod = false,
)

internal val FpxRequirement = PaymentMethodRequirements(
    paymentIntentSupport = PaymentIntentSupport.AlwaysSupported,
    setupIntentSupport = SetupIntentSupport.NotSupported,
    canBeUsedAsSavedPaymentMethod = false,
)

internal val AlipayRequirement = PaymentMethodRequirements(
    paymentIntentSupport = PaymentIntentSupport.AlwaysSupported,
    setupIntentSupport = SetupIntentSupport.NotSupported,
    canBeUsedAsSavedPaymentMethod = false,
)
