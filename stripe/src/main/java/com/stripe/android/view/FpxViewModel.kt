package com.stripe.android.view

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.liveData
import com.stripe.android.ApiRequest
import com.stripe.android.PaymentConfiguration
import com.stripe.android.StripeApiRepository
import com.stripe.android.model.FpxBankStatuses
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal class FpxViewModel @JvmOverloads internal constructor(
    application: Application,
    workDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {
    private val publishableKey = PaymentConfiguration.getInstance(application).publishableKey
    private val stripeRepository = StripeApiRepository(
        application,
        publishableKey,
        workDispatcher = workDispatcher
    )

    internal var selectedPosition: Int? = null

    @JvmSynthetic
    internal fun getFpxBankStatues() = liveData<FpxBankStatuses> {
        emit(
            runCatching {
                stripeRepository.getFpxBankStatus(ApiRequest.Options(publishableKey))
            }.getOrDefault(FpxBankStatuses())
        )
    }
}
