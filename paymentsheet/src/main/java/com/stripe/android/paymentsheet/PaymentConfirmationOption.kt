package com.stripe.android.paymentsheet

import android.os.Parcelable
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams
import kotlinx.parcelize.Parcelize
import com.stripe.android.model.PaymentMethod as PaymentMethodModel

internal sealed interface PaymentConfirmationOption<TArgs : Parcelable> : Parcelable {
    val arguments: TArgs

    @Parcelize
    data class GooglePay(
        override val arguments: Args,
    ) : PaymentConfirmationOption<GooglePay.Args> {
        @Parcelize
        data class Args(
            val environment: PaymentSheet.GooglePayConfiguration.Environment?,
            val merchantName: String,
            val merchantCountryCode: String,
            val merchantCurrencyCode: String?,
            val customAmount: Long?,
            val customLabel: String?,
            val billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
        ) : Parcelable
    }

    @Parcelize
    data class ExternalPaymentMethod(
        override val arguments: Args,
    ) : PaymentConfirmationOption<ExternalPaymentMethod.Args> {
        @Parcelize
        data class Args(
            val type: String,
            val billingDetails: PaymentMethodModel.BillingDetails?,
        ) : Parcelable
    }

    @Parcelize
    sealed interface PaymentMethod<TArgs : Parcelable> : PaymentConfirmationOption<TArgs> {
        @Parcelize
        data class New(
            override val arguments: Args,
        ) : PaymentMethod<New.Args> {
            @Parcelize
            data class Args(
                val createParams: PaymentMethodCreateParams,
                val optionsParams: PaymentMethodOptionsParams?,
                val shouldSave: Boolean
            ) : Parcelable
        }

        @Parcelize
        data class Saved(
            override val arguments: Args,
        ) : PaymentMethod<Saved.Args> {
            @Parcelize
            data class Args(
                val paymentMethod: PaymentMethodModel,
                val optionsParams: PaymentMethodOptionsParams?,
            ) : Parcelable
        }
    }
}
