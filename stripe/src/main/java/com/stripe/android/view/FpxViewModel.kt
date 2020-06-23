package com.stripe.android.view

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.stripe.android.ApiRequest
import com.stripe.android.PaymentConfiguration
import com.stripe.android.StripeApiRepository
import com.stripe.android.model.FpxBankStatuses
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren

internal class FpxViewModel @JvmOverloads internal constructor(
    application: Application,
    private val workDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {
    private val paymentConfiguration = PaymentConfiguration.getInstance(application)
    private val publishableKey = paymentConfiguration.publishableKey
    private val stripeRepository = StripeApiRepository(
        application,
        publishableKey,
        workDispatcher = workDispatcher,
        betas = paymentConfiguration.betas
    )

    internal var selectedPosition: Int? = null

    @JvmSynthetic
    internal fun getFpxBankStatues() = liveData<FpxBankStatuses>(workDispatcher) {
        emitSource(
            runCatching {
                stripeRepository.getFpxBankStatus(ApiRequest.Options(publishableKey))
            }.getOrDefault(MutableLiveData(FpxBankStatuses()))
        )
    }

    override fun onCleared() {
        super.onCleared()
        workDispatcher.cancelChildren()
    }
}
