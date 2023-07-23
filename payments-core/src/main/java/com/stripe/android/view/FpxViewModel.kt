package com.stripe.android.view

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.BankStatuses
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.networking.StripeRepository

internal class FpxViewModel internal constructor(
    application: Application,
    private val publishableKey: String,
    private val stripeRepository: StripeRepository
) : AndroidViewModel(application) {
    internal var selectedPosition: Int? = null

    @JvmSynthetic
    internal fun getFpxBankStatues() = liveData {
        emit(
            stripeRepository.getFpxBankStatus(
                options = ApiRequest.Options(publishableKey),
            ).getOrElse {
                BankStatuses()
            }
        )
    }

    internal class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val publishableKey = PaymentConfiguration.getInstance(application).publishableKey
            val stripeRepository = StripeApiRepository(
                application,
                { publishableKey }
            )

            return FpxViewModel(application, publishableKey, stripeRepository) as T
        }
    }
}
