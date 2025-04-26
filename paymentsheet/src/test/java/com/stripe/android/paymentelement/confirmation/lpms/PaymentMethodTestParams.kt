package com.stripe.android.paymentelement.confirmation.lpms

import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.lpms.foundations.network.MerchantCountry
import com.stripe.android.paymentelement.confirmation.lpms.foundations.network.StripeNetworkTestClient

internal sealed interface PaymentMethodTestParams {
    data class ConfirmParams(
        val customerId: String?,
        val option: PaymentMethodConfirmationOption,
    )

    suspend fun create(
        country: MerchantCountry,
        client: StripeNetworkTestClient
    ): Result<ConfirmParams>

    data class New(
        val createParams: PaymentMethodCreateParams,
        val optionsParams: PaymentMethodOptionsParams? = null,
        val extraParams: PaymentMethodExtraParams? = null,
        val customerRequestedSave: Boolean = false,
    ) : PaymentMethodTestParams {
        override suspend fun create(
            country: MerchantCountry,
            client: StripeNetworkTestClient
        ): Result<ConfirmParams> {
            return Result.success(
                ConfirmParams(
                    customerId = null,
                    option = PaymentMethodConfirmationOption.New(
                        createParams = createParams,
                        optionsParams = optionsParams,
                        extraParams = extraParams,
                        shouldSave = customerRequestedSave,
                    ),
                )
            )
        }
    }

    data class Saved(
        val optionsParams: PaymentMethodOptionsParams? = null,
    ) : PaymentMethodTestParams {
        override suspend fun create(
            country: MerchantCountry,
            client: StripeNetworkTestClient
        ): Result<ConfirmParams> = kotlin.runCatching {
            val savedPm = client.retrievePaymentMethod(country).getOrThrow()

            return Result.success(
                ConfirmParams(
                    customerId = savedPm.customerId,
                    option = PaymentMethodConfirmationOption.Saved(
                        paymentMethod = savedPm.paymentMethod,
                        optionsParams = optionsParams,
                    )
                )
            )
        }
    }
}
