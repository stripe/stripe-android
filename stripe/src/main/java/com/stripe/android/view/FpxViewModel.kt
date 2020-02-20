package com.stripe.android.view

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.ApiRequest
import com.stripe.android.PaymentConfiguration
import com.stripe.android.StripeApiRepository
import com.stripe.android.StripeRepository
import com.stripe.android.model.FpxBankStatuses
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class FpxViewModel internal constructor(
    application: Application,
    private val workScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val publishableKey = PaymentConfiguration.getInstance(context).publishableKey

    private val internalFpxBankStatuses: MutableLiveData<FpxBankStatuses> = MutableLiveData()

    internal val fpxBankStatuses: LiveData<FpxBankStatuses>
        @JvmSynthetic
        get() {
            return internalFpxBankStatuses
        }

    @JvmSynthetic
    internal fun loadFpxBankStatues() {
        val stripeRepository: StripeRepository = StripeApiRepository(context, publishableKey)
        val paymentConfiguration = PaymentConfiguration.getInstance(context)
        workScope.launch {
            val fpxBankStatuses = try {
                stripeRepository.getFpxBankStatus(
                    ApiRequest.Options(paymentConfiguration.publishableKey))
            } catch (e: Exception) {
                FpxBankStatuses()
            }

            withContext(Main) {
                fpxBankStatuses.let {
                    this@FpxViewModel.internalFpxBankStatuses.value = it
                }
            }
        }
    }

    internal class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return FpxViewModel(application) as T
        }
    }
}
