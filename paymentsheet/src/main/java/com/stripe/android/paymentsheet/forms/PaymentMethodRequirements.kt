package com.stripe.android.paymentsheet.forms

internal sealed interface Requirement
internal sealed interface PIRequirement : Requirement
internal sealed interface SIRequirement : Requirement

/**
 * This requirement is dependent on the configuration passed by the app to the SDK.
 */
internal object Delayed : PIRequirement, SIRequirement

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
     * This indicates if the payment method can be confirmed from a payment method.
     *  - Null means that it is not supported, or that it is attached as a different type
     *  - false means that it is supported by the payment method, but not currently enabled
     *  (likely because of a lack of mandate support)
     *  - true means that a PM of this type attached to a customer can be confirmed
     */
    val confirmPMFromCustomer: Boolean?,
)
