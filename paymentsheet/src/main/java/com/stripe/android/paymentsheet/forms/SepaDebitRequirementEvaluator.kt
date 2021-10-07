package com.stripe.android.paymentsheet.forms

internal object SepaDebitRequirementEvaluator : RequirementEvaluator(
    piRequirements = setOf(Delayed),

    /**
     * This check is used in the needed places where we want to enforce a block
     * until there is a way of retrieving valid mandates associated with a customer PM.
     *
     * This is just a artificial block we put in to make it easy to explain
     * that we are not allowing SetupIntent, or PaymentIntent with setupFuture
     * usage, even though this is possible (both with and without a customer).
     * The reason we are excluding it is because after PI w/SFU set or PI
     * is used, the payment method appears as a SEPA payment method attached
     * to a customer.  Without this block the SEPA payment method would
     * show in PaymentSheet.  If the user used this save payment method
     * we would have no way to know if the existing mandate was valid or how
     * to request the user to re-accept the mandate.
     *
     * So to simplify the description to users, we will not show SI as available.
     */
    siRequirements = null
    // once supported it should be: setOf(Delayed)
)
