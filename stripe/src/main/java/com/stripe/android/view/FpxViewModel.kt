package com.stripe.android.view

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.stripe.android.ApiRequest
import com.stripe.android.PaymentConfiguration
import com.stripe.android.StripeApiRepository
import com.stripe.android.model.FpxBankStatuses
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren

internal class FpxViewModel @JvmOverloads internal constructor(
    application: Application,
    private val workContext: CoroutineContext = Dispatchers.IO
) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val publishableKey = PaymentConfiguration.getInstance(context).publishableKey
    private val stripeRepository = StripeApiRepository(context, publishableKey)

    internal var selectedPosition: Int? = null

    @JvmSynthetic
    internal fun getFpxBankStatues() = liveData<FpxBankStatuses>(workContext) {
        emitSource(
            runCatching {
                stripeRepository.getFpxBankStatus(ApiRequest.Options(publishableKey))
            }.getOrDefault(MutableLiveData(FpxBankStatuses()))
        )
    }

    override fun onCleared() {
        super.onCleared()
        workContext.cancelChildren()
    }
}
