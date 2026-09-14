@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout.settings

internal object CheckoutSavedPaymentMethodScenarios {
    private val session = CheckoutPlaygroundDefinitions.session

    val group = group(
        "saved_payment_methods",
        "Saved payment methods",
        leaf("guest", "Guest") {
            set(session.customer, CheckoutCustomer.Guest)
        },
        leaf("new_save_enabled", "New customer — saving enabled") {
            set(session.customer, CheckoutCustomer.New)
            set(session.paymentMethodSave, true)
        },
        leaf("new_save_disabled", "New customer — saving disabled") {
            set(session.customer, CheckoutCustomer.New)
            set(session.paymentMethodSave, false)
        },
        group(
            "returning_customer",
            "Returning customer",
            returningCustomerLeaf("save_remove", "Save and remove"),
            returningCustomerLeaf("save_only", "Save only", remove = false),
            returningCustomerLeaf("remove_only", "Remove only", save = false),
            returningCustomerLeaf("view_only", "View only", save = false, remove = false),
        ),
    )

    private fun returningCustomerLeaf(
        key: String,
        name: String,
        save: Boolean = true,
        remove: Boolean = true,
    ) = leaf(key, name) {
        set(session.customer, CheckoutCustomer.Returning)
        set(session.customerId, null)
        set(session.paymentMethodSave, save)
        set(session.paymentMethodRemove, remove)
    }
}
