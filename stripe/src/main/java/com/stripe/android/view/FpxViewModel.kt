package com.stripe.android.view

import android.app.Application
import android.os.AsyncTask
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.stripe.android.ApiRequest
import com.stripe.android.PaymentConfiguration
import com.stripe.android.StripeApiRepository
import com.stripe.android.StripeRepository
import com.stripe.android.model.FpxBankStatuses

internal class FpxViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext

    @JvmSynthetic
    internal val fpxBankStatuses: MutableLiveData<FpxBankStatuses> = MutableLiveData()

    @JvmSynthetic
    internal fun loadFpxBankStatues() {
        val stripeRepository: StripeRepository = StripeApiRepository(context)
        val paymentConfiguration = PaymentConfiguration.getInstance(context)
        AsyncTask.execute {
            val fpxBankStatuses = try {
                stripeRepository.getFpxBankStatus(
                    ApiRequest.Options.create(paymentConfiguration.publishableKey))
            } catch (e: Exception) {
                null
            }

            fpxBankStatuses?.let { this.fpxBankStatuses.postValue(it) }
        }
    }
}
