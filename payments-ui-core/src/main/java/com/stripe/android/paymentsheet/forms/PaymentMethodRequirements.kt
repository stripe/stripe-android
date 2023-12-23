package com.stripe.android.paymentsheet.forms

import androidx.annotation.RestrictTo
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class PaymentMethodRequirements(

    /**
     * These are the requirements for using a PaymentIntent.
     *  - Only [PIRequirement]s are allowed in this set.
     * - If this is null, PaymentIntents (even if SFU is set) are not supported by this LPM.
     */
    @SerialName("pi_requirements")
    val piRequirements: Set<PIRequirement>?,

    /**
     * These are the requirements for using a SetupIntent.
     *   - Only [SIRequirement]s are allowed in this set.
     *   - If this is null SetupIntents and PaymentIntents with SFU set are not
     *   supported by this LPM. If SetupIntents are supported, but there are
     *   no additional requirements this must be an emptySet.
     *   - In order to make sure the PM can be used when attached to a customer it
     *   must include the requirements of the saved payment method.  For instance,
     *   Bancontact is not delayed, but when saved it is represented as a SEPA payment
     *   method which is delayed.  So there must be Delay support in order to meet
     *   the requirements of this PM.  (There was a consideration of adding a SaveType
     *   that in cases where SI or PIw/SFU it would also check the requirements of
     *   the SaveType - not sure if the SaveType pi and/or si requirements should be checked).
     */
    @SerialName("si_requirements")
    val siRequirements: Set<SIRequirement>?,

    /**
     * This indicates if the payment method can be confirmed when attached to a customer
     * and only the Payment Method id is available. This also implies that the PaymentMethod
     * is internally supported in the SDK so it can be parsed in the customer repository requests.
     *  - Null means that it is not supported, or that it is attached as a different type
     *  - false means that it is supported by the payment method, but not currently enabled
     *  (likely because of a lack of mandate support)
     *  - true means that a PM of this type attached to a customer can be confirmed
     */
    @SerialName("confirm_pm_from_customer")
    private val confirmPMFromCustomer: Boolean?
) {
    // Confirming from a customer assumes that we can parse the PaymentMethod
    fun getConfirmPMFromCustomer(code: PaymentMethodCode) =
        (PaymentMethod.Type.fromCode(code) != null) && (confirmPMFromCustomer == true)
}

internal val CardRequirement = PaymentMethodRequirements(
    piRequirements = emptySet(),
    siRequirements = emptySet(),
    confirmPMFromCustomer = true
)

internal val BancontactRequirement = PaymentMethodRequirements(
    piRequirements = emptySet(),
    siRequirements = setOf(Delayed),

    /**
     * PM will be attached as a SEPA Debit payment method and have the requirements
     * of that PaymentMethod.
     */
    confirmPMFromCustomer = true,
)

internal val SofortRequirement = PaymentMethodRequirements(
    piRequirements = setOf(Delayed),
    siRequirements = setOf(Delayed),

    /**
     * PM will be attached as a SEPA Debit payment method and have the requirements
     * of that PaymentMethod.
     */
    confirmPMFromCustomer = true,
)

internal val IdealRequirement = PaymentMethodRequirements(
    piRequirements = emptySet(),
    siRequirements = setOf(Delayed),

    /**
     * PM will be attached as a SEPA Debit payment method and have the requirements
     * of that PaymentMethod.
     */
    confirmPMFromCustomer = true,
)

internal val SepaDebitRequirement = PaymentMethodRequirements(
    piRequirements = setOf(Delayed),
    siRequirements = setOf(Delayed),

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
    confirmPMFromCustomer = true
)

internal val EpsRequirement = PaymentMethodRequirements(
    piRequirements = emptySet(),
    siRequirements = null, // this is not supported by this payment method
    confirmPMFromCustomer = null
)

internal val P24Requirement = PaymentMethodRequirements(
    piRequirements = emptySet(),
    siRequirements = null, // this is not supported by this payment method

    /**
     * This cannot be saved to a customer object.
     */
    confirmPMFromCustomer = null
)

internal val GiropayRequirement = PaymentMethodRequirements(
    piRequirements = emptySet(),
    siRequirements = null, // this is not supported by this payment method
    confirmPMFromCustomer = null
)

/**
 * This defines the requirements for usage as a Payment Method.
 */
internal val AfterpayClearpayRequirement = PaymentMethodRequirements(
    piRequirements = setOf(ShippingAddress),
    /**
     * SetupIntents are not supported by this payment method, in addition,
     * setup intents do not have shipping information
     */
    siRequirements = null,
    confirmPMFromCustomer = null
)

/**
 * This defines the requirements for usage as a Payment Method.
 */
internal val KlarnaRequirement = PaymentMethodRequirements(
    piRequirements = emptySet(),
    siRequirements = null,
    confirmPMFromCustomer = null
)

/**
 * This defines the requirements for usage as a Payment Method.
 */
internal val PaypalRequirement = PaymentMethodRequirements(
    piRequirements = emptySet(),
    siRequirements = emptySet(),
    confirmPMFromCustomer = true
)

/**
 * This defines the requirements for usage as a Payment Method.
 */
internal val AffirmRequirement = PaymentMethodRequirements(
    piRequirements = setOf(ShippingAddress),
    siRequirements = null,
    confirmPMFromCustomer = null
)

internal val RevolutPayRequirement = PaymentMethodRequirements(
    piRequirements = emptySet(),
    siRequirements = emptySet(),
    confirmPMFromCustomer = true
)

internal val AmazonPayRequirement = PaymentMethodRequirements(
    piRequirements = emptySet(),
    siRequirements = null,
    confirmPMFromCustomer = null
)

internal val AlmaRequirement = PaymentMethodRequirements(
    piRequirements = emptySet(),
    siRequirements = null,
    confirmPMFromCustomer = null
)

internal val MobilePayRequirement = PaymentMethodRequirements(
    piRequirements = emptySet(),
    siRequirements = null,
    confirmPMFromCustomer = null
)

/**
 * This defines the requirements for usage as a Payment Method.
 */
internal val AuBecsDebitRequirement = PaymentMethodRequirements(
    piRequirements = setOf(Delayed),
    siRequirements = setOf(Delayed),
    confirmPMFromCustomer = true
)

/**
 * This defines the requirements for usage as a Payment Method.
 */
internal val BacsDebitRequirement = PaymentMethodRequirements(
    piRequirements = setOf(Delayed),
    siRequirements = null,
    confirmPMFromCustomer = null
)

internal val ZipRequirement = PaymentMethodRequirements(
    piRequirements = emptySet(),
    siRequirements = null,
    confirmPMFromCustomer = null
)

/**
 * This defines the requirements for usage as a Payment Method.
 */
internal val USBankAccountRequirement = PaymentMethodRequirements(
    piRequirements = setOf(Delayed),
    siRequirements = setOf(Delayed),
    confirmPMFromCustomer = true
)

internal val UpiRequirement = PaymentMethodRequirements(
    piRequirements = emptySet(),
    siRequirements = null,
    confirmPMFromCustomer = null
)

internal val BlikRequirement = PaymentMethodRequirements(
    piRequirements = emptySet(),
    siRequirements = null,
    confirmPMFromCustomer = null
)

internal val CashAppPayRequirement = PaymentMethodRequirements(
    piRequirements = emptySet(),
    siRequirements = emptySet(),
    confirmPMFromCustomer = true,
)

internal val GrabPayRequirement = PaymentMethodRequirements(
    piRequirements = emptySet(),
    siRequirements = null,
    confirmPMFromCustomer = false,
)

internal val FpxRequirement = PaymentMethodRequirements(
    piRequirements = emptySet(),
    siRequirements = null,
    confirmPMFromCustomer = false,
)

internal val AlipayRequirement = PaymentMethodRequirements(
    piRequirements = emptySet(),
    siRequirements = null,
    confirmPMFromCustomer = false,
)

internal val OxxoRequirement = PaymentMethodRequirements(
    piRequirements = setOf(Delayed),
    siRequirements = null,
    confirmPMFromCustomer = false,
)

internal val BoletoRequirement = PaymentMethodRequirements(
    piRequirements = setOf(Delayed),
    siRequirements = setOf(Delayed),
    confirmPMFromCustomer = true,
)

internal val KonbiniRequirement = PaymentMethodRequirements(
    piRequirements = setOf(Delayed),
    siRequirements = null,
    confirmPMFromCustomer = null,
)

internal val SwishRequirement = PaymentMethodRequirements(
    piRequirements = emptySet(),
    siRequirements = null,
    confirmPMFromCustomer = false,
)
