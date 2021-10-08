package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.PaymentSheet

internal object BancontactRequirementEvaluator : RequirementEvaluator(
    piRequirements = emptySet(),

    /**
     * This PM is blocked for SI or PI w/SFU set until there is a way of retrieving
     * valid mandates associated with a customer PM. Once mandates are supported it will
     * still be blocked if the SDK does not support Delayed PM.
     *
     * This is just a artificial block we put in to make it easy to explain
     * that we are not allowing SetupIntent, or PaymentIntent with setupFuture
     * usage, even though this is technically possible (both with and without a customer in the intent).
     *
     * The reason we are excluding it is because if the PM could be saved to the customer object
     * then it should be possible from PaymentSheet to select and use it to confirm when
     * retrieved from a customer object.
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
     * [PaymentSheet.Configuration.allowsDelayedPaymentMethods] support as indicated in
     * the configuration.
     */
    siRequirements = null, // emptySet(Delayed)
    confirmPMFromCustomer = false
)
