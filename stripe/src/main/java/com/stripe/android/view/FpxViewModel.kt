package com.stripe.android.view

import android.app.Application
import android.os.AsyncTask
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stripe.android.ApiRequest
import com.stripe.android.PaymentConfiguration
import com.stripe.android.StripeApiRepository
import com.stripe.android.StripeRepository
import com.stripe.android.model.FpxBankStatuses

internal class FpxViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext

    private val internalFpxBankStatuses: MutableLiveData<FpxBankStatuses> = MutableLiveData()

    internal val fpxBankStatuses: LiveData<FpxBankStatuses>
        @JvmSynthetic
        get() {
            return internalFpxBankStatuses
        }

    @JvmSynthetic
    internal fun loadFpxBankStatues() {
        val stripeRepository: StripeRepository = StripeApiRepository(context)
        val paymentConfiguration = PaymentConfiguration.getInstance(context)
        AsyncTask.execute {
            val fpxBankStatuses = try {
                stripeRepository.getFpxBankStatus(
                    ApiRequest.Options(paymentConfiguration.publishableKey))
            } catch (e: Exception) {
                null
            }

            fpxBankStatuses?.let { this.internalFpxBankStatuses.postValue(it) }
        }
    }
}
