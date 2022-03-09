package com.stripe.android.paymentsheet.forms

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

internal sealed interface Requirement : Parcelable
internal sealed interface PIRequirement : Requirement
internal sealed interface SIRequirement : Requirement

/**
 * This requirement is dependent on the configuration passed by the app to the SDK.
 */
@Parcelize
internal object Delayed : PIRequirement, SIRequirement

/**
 * The Payment Method requires a shipping address in the Payment Intent.
 * The fields required are name, address line 1, country, and postal code.
 */
@Parcelize
internal object ShippingAddress : PIRequirement

@Parcelize
internal data class PaymentMethodRequirements(

    /**
     * These are the requirements for using a PaymentIntent.
     *  - Only [PIRequirement]s are allowed in this set.
     * - If this is null, PaymentIntents (even if SFU is set) are not supported by this LPM.
     */
    val piRequirements: Set<PIRequirement>?,

    /**
     * These are the requirements for using a SetupIntent.
     *   - Only [SIRequirement]s are allowed in this set.
     *   - If this is null SetupIntents and PaymentIntents with SFU set are not
     *   supported by this LPM. If SetupIntents are supported, but there are
     *   no additional requirements this must be an emptySet.
     *   - In order to make sure the PM can be used when attached to a customer it
     *   must include the requirements of the saved payment method.  For instance,
     *   Bancontact is not delayed, but when saved it is represented as a SEPA paymnent
     *   method which is delayed.  So there must be Delay support in order to meet
     *   the requiremetns of this PM.  (There was a consideration of adding a SaveType
     *   that in cases where SI or PIw/SFU it would also check the requirements of
     *   the SaveType - not sure if the SaveType pi and/or si requirements should be checked).
     */
    val siRequirements: Set<SIRequirement>?,

    /**
     * This indicates if the payment method can be confirmed when attached to a customer
     * and only the Payment Method id is available.
     *  - Null means that it is not supported, or that it is attached as a different type
     *  - false means that it is supported by the payment method, but not currently enabled
     *  (likely because of a lack of mandate support)
     *  - true means that a PM of this type attached to a customer can be confirmed
     */
    val confirmPMFromCustomer: Boolean?,
) : Parcelable

internal val CardRequirement = PaymentMethodRequirements(
    piRequirements = emptySet(),
    siRequirements = emptySet(),
    confirmPMFromCustomer = true
)

internal val BancontactRequirement = PaymentMethodRequirements(
    piRequirements = emptySet(),

    /**
     * Currently we will not support this PaymentMethod for use with PI w/SFU,
     * or SI until there is a way of retrieving valid mandates associated with a customer PM.
     *
     * The reason we are excluding it is because after PI w/SFU set or PI
     * is used, the payment method appears as a SEPA payment method attached
     * to a customer.  Without this block the SEPA payment method would
     * show in PaymentSheet.  If the user used this save payment method
     * we would have no way to know if the existing mandate was valid or how
     * to request the user to re-accept the mandate.
     *
     * SEPA Debit does support PI w/SFU and SI (both with and without a customer),
     * and it is Delayed in this configuration.
     */
    siRequirements = null,

    /**
     * This PM cannot be attached to a customer, it should be noted that it
     * will be attached as a SEPA Debit payment method and have the requirements
     * of that PaymentMethod, but for now SEPA is not supported either so we will
     * call it false.
     */
    confirmPMFromCustomer = false
)

internal val SofortRequirement = PaymentMethodRequirements(
    piRequirements = setOf(Delayed),

    /**
     * Currently we will not support this PaymentMethod for use with PI w/SFU,
     * or SI until there is a way of retrieving valid mandates associated with a customer PM.
     *
     * The reason we are excluding it is because after PI w/SFU set or PI
     * is used, the payment method appears as a SEPA payment method attached
     * to a customer.  Without this block the SEPA payment method would
     * show in PaymentSheet.  If the user used this save payment method
     * we would have no way to know if the existing mandate was valid or how
     * to request the user to re-accept the mandate.
     *
     * SEPA Debit does support PI w/SFU and SI (both with and without a customer),
     * and it is Delayed in this configuration.
     */
    siRequirements = null,

    /**
     * This PM cannot be attached to a customer, it should be noted that it
     * will be attached as a SEPA Debit payment method and have the requirements
     * of that PaymentMethod, but for now SEPA is not supported either so we will
     * call it false.
     */
    confirmPMFromCustomer = false
)

internal val IdealRequirement = PaymentMethodRequirements(
    piRequirements = emptySet(),

    /**
     * Currently we will not support this PaymentMethod for use with PI w/SFU,
     * or SI until there is a way of retrieving valid mandates associated with a customer PM.
     *
     * The reason we are excluding it is because after PI w/SFU set or PI
     * is used, the payment method appears as a SEPA payment method attached
     * to a customer.  Without this block the SEPA payment method would
     * show in PaymentSheet.  If the user used this save payment method
     * we would have no way to know if the existing mandate was valid or how
     * to request the user to re-accept the mandate.
     *
     * SEPA Debit does support PI w/SFU and SI (both with and without a customer),
     * and it is Delayed in this configuration.
     */
    siRequirements = null,

    /**
     * This PM cannot be attached to a customer, it should be noted that it
     * will be attached as a SEPA Debit payment method and have the requirements
     * of that PaymentMethod, but for now SEPA is not supported either so we will
     * call it false.
     */
    confirmPMFromCustomer = false
)

internal val SepaDebitRequirement = PaymentMethodRequirements(
    piRequirements = setOf(Delayed),

    /**
     * Currently we will not support this PaymentMethod for use with PI w/SFU,
     * or SI until there is a way of retrieving valid mandates associated with a customer PM.
     *
     * The reason we are excluding it is because after PI w/SFU set or PI
     * is used, the payment method appears as a SEPA payment method attached
     * to a customer.  Without this block the SEPA payment method would
     * show in PaymentSheet.  If the user used this save payment method
     * we would have no way to know if the existing mandate was valid or how
     * to request the user to re-accept the mandate.
     *
     * SEPA Debit does support PI w/SFU and SI (both with and without a customer),
     * and it is Delayed in this configuration.
     */
    siRequirements = null,

    /**
     * This PM is blocked for use from a customer PM.  Once it is possible to retrieve a
     * mandate from a customer PM for use on confirm the SDK will be able to support this
     * scenario.
     *
     * Here we explain the details
     * - if PI w/SFU set or SI with a customer, or
     * - if PI w/SFU set or SI with/out a customer and later attached when used with
     * a webhook
     * (Note: from the client there is no way to detect if a PI or SI is associated with a customer)
     *
     * then, this payment method would be attached to the customer as a SEPA payment method.
     * (Note: Bancontact, iDEAL, and Sofort require authentication, but SEPA does not.
     * also Bancontact, iDEAL are not delayed, but Sofort and SEPA are delayed.)
     *
     * The SEPA payment method requires a mandate when confirmed. Currently there is no
     * way with just a client_secret and public key to get a valid mandate associated with
     * a customers payment method that can be used on confirmation.
     *
     * Even with mandate support, in order to make sure that any payment method added can
     * also be used when attached to a customer, this LPM will require
     * [PaymentSheet.Configuration].allowsDelayedPaymentMethods support as indicated in
     * the configuration.
     */
    confirmPMFromCustomer = false
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

    /**
     * SetupIntents are not supported by this payment method. Currently, for paypal (and others see
     * oof #5) customers are not able to set up saved payment methods for reuse. The API errors if
     * confirming PI+SFU or SI with these methods.
     */
    siRequirements = null,
    confirmPMFromCustomer = null
)

/**
 * This defines the requirements for usage as a Payment Method.
 */
internal val AffirmRequirement = PaymentMethodRequirements(
    piRequirements = setOf(ShippingAddress),
    siRequirements = null,
    confirmPMFromCustomer = null
)

/**
 * This defines the requirements for usage as a Payment Method.
 */
internal val AuBecsDebitRequirement = PaymentMethodRequirements(
    piRequirements = emptySet(),
    siRequirements = emptySet(),
    confirmPMFromCustomer = null
)
