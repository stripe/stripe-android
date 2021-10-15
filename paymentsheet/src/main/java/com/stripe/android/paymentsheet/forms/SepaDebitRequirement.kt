package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.PaymentSheet

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
