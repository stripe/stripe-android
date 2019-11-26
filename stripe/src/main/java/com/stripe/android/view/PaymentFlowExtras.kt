package com.stripe.android.view

import com.stripe.android.PaymentSessionConfig

/**
 * See [PaymentSessionConfig.Builder.setShippingInformationValidator] and
 * [PaymentSessionConfig.Builder.setShippingMethodsFactory]
 */
object PaymentFlowExtras {
    /**
     * Implement [PaymentSessionConfig.ShippingMethodsFactory.create]
     */
    @Deprecated("See PaymentSessionConfig.ShippingMethodsFactory")
    const val EXTRA_DEFAULT_SHIPPING_METHOD: String = "default_shipping_method"

    /**
     * Implement [PaymentSessionConfig.ShippingInformationValidator.isValid]
     */
    @Deprecated("See PaymentSessionConfig.ShippingInformationValidator")
    const val EXTRA_IS_SHIPPING_INFO_VALID: String = "shipping_is_shipping_info_valid"

    /**
     * Implement [PaymentSessionConfig.ShippingInformationValidator]
     */
    @Deprecated("See PaymentSessionConfig.ShippingInformationValidator")
    const val EXTRA_SHIPPING_INFO_DATA: String = "shipping_info_data"

    /**
     * Implement [PaymentSessionConfig.ShippingInformationValidator.getErrorMessage]
     */
    @Deprecated("See PaymentSessionConfig.ShippingInformationValidator")
    const val EXTRA_SHIPPING_INFO_ERROR: String = "shipping_info_error"

    /**
     * Implement [PaymentSessionConfig.ShippingInformationValidator]
     */
    @Deprecated("See PaymentSessionConfig.ShippingInformationValidator")
    const val EVENT_SHIPPING_INFO_PROCESSED: String = "shipping_info_processed"

    /**
     * Implement [PaymentSessionConfig.ShippingInformationValidator]
     */
    @Deprecated("See PaymentSessionConfig.ShippingInformationValidator")
    const val EVENT_SHIPPING_INFO_SUBMITTED: String = "shipping_info_submitted"

    /**
     * Implement [PaymentSessionConfig.ShippingMethodsFactory.create]
     */
    @Deprecated("See PaymentSessionConfig.ShippingMethodsFactory")
    const val EXTRA_VALID_SHIPPING_METHODS: String = "valid_shipping_methods"
}
