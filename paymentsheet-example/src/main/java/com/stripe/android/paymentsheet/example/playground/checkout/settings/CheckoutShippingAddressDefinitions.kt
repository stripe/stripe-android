package com.stripe.android.paymentsheet.example.playground.checkout.settings

import com.stripe.android.core.utils.FeatureFlags

internal object CheckoutShippingAddressDefinitions {
    val shouldSetConfiguration = boolean(
        key = "controller.shipping_address.should_set_configuration",
        displayName = "Set configuration",
    )
    val enableAddressElementUnsavedChanges = boolean(
        key = "controller.shipping_address.enable_unsaved_changes",
        displayName = "Unsaved changes dialog (WIP)",
        defaultValue = false,
        applyFeatureFlags = FeatureFlags.enableAddressElementUnsavedChanges::setEnabled,
    )

    val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
        key = "controller.shipping_address",
        displayName = "Shipping Address Element",
        children = arrayOf(shouldSetConfiguration, enableAddressElementUnsavedChanges),
    )
}
