package com.stripe.android.view

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.FpxBankStatuses
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.networking.StripeRepository

internal class FpxViewModel internal constructor(
    application: Application,
    private val publishableKey: String,
    private val stripeRepository: StripeRepository
) : AndroidViewModel(application) {
    internal var selectedPosition: Int? = null

    @JvmSynthetic
    internal fun getFpxBankStatues() = liveData<FpxBankStatuses> {
        emit(
            runCatching {
                stripeRepository.getFpxBankStatus(ApiRequest.Options(publishableKey))
            }.getOrDefault(FpxBankStatuses())
        )
    }

    internal class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val publishableKey = PaymentConfiguration.getInstance(application).publishableKey
            val stripeRepository = StripeApiRepository(
                application,
                publishableKey
            )

            return FpxViewModel(application, publishableKey, stripeRepository) as T
        }
    }
}
