package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.utils.filterNotNullValues

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface CreateFinancialConnectionsSessionParams {
    fun toMap(): Map<String, Any>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class InstantDebits(
        val clientSecret: String,
        val customerEmailAddress: String?,
        val hostedSurface: String?,
        val linkMode: LinkMode?,
    ) : CreateFinancialConnectionsSessionParams {

        override fun toMap(): Map<String, Any> {
            val paymentMethod = PaymentMethodCreateParams(
                type = PaymentMethod.Type.Link,
                billingDetails = PaymentMethod.BillingDetails(
                    email = customerEmailAddress,
                ),
            )
            return mapOf(
                PARAM_CLIENT_SECRET to clientSecret,
                PARAM_HOSTED_SURFACE to hostedSurface,
                PARAM_PRODUCT to "instant_debits",
                PARAM_ATTACH_REQUIRED to true,
                PARAM_LINK_MODE to linkMode.valueForHostedSurface(hostedSurface),
                PARAM_PAYMENT_METHOD_DATA to paymentMethod.toParamMap()
            ).filterNotNullValues()
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class USBankAccount(
        val clientSecret: String,
        val customerName: String,
        val customerEmailAddress: String?,
        val hostedSurface: String?,
        val linkMode: LinkMode?,
    ) : CreateFinancialConnectionsSessionParams {
        override fun toMap(): Map<String, Any> {
            val paymentMethod = PaymentMethodCreateParams.createUSBankAccount(
                billingDetails = PaymentMethod.BillingDetails(
                    name = customerName,
                    email = customerEmailAddress
                )
            )
            return mapOf(
                PARAM_CLIENT_SECRET to clientSecret,
                PARAM_HOSTED_SURFACE to hostedSurface,
                PARAM_LINK_MODE to linkMode.valueForHostedSurface(hostedSurface),
                PARAM_PAYMENT_METHOD_DATA to paymentMethod.toParamMap(),
                // TODO: Remove testing code
                "initial_institution" to "bcinst_Jg18xEfPHevfHP",
            ).filterNotNullValues()
        }
    }

    private companion object {
        const val PARAM_CLIENT_SECRET = "client_secret"
        const val PARAM_HOSTED_SURFACE = "hosted_surface"
        const val PARAM_ATTACH_REQUIRED = "attach_required"
        const val PARAM_PRODUCT = "product"
        const val PARAM_PAYMENT_METHOD_DATA = "payment_method_data"
        const val PARAM_LINK_MODE = "link_mode"
    }
}

private fun LinkMode?.valueForHostedSurface(hostedSurface: String?): String? {
    // This is to align with the Web behavior, where we only send the link_mode
    // for flows hosted in a Stripe-owned surface. To make sure a value is sent even if link_mode
    // is null, we default to LINK_DISABLED.
    return if (hostedSurface != null) {
        this?.value ?: "LINK_DISABLED"
    } else {
        null
    }
}
