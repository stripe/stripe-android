package com.stripe.android.customersheet.util

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.customersheet.CustomerSheetIntegration
import com.stripe.android.customersheet.data.CustomerSheetInitializationDataSource
import com.stripe.android.customersheet.data.CustomerSheetIntentDataSource
import com.stripe.android.customersheet.data.CustomerSheetPaymentMethodDataSource
import com.stripe.android.customersheet.data.CustomerSheetSavedSelectionDataSource
import com.stripe.android.customersheet.data.injection.DaggerCustomerAdapterDataSourceComponent
import com.stripe.android.customersheet.data.injection.DaggerCustomerSessionDataSourceComponent
import com.stripe.android.paymentsheet.ExperimentalCustomerSessionApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

/**
 * This objects holds references to objects that need to be shared across Activity boundaries
 * but can't be serialized, or objects that can't be injected where they are used.
 */
@OptIn(ExperimentalCustomerSessionApi::class)
internal object CustomerSheetHacks {
    private val _initializationDataSource = MutableStateFlow<CustomerSheetInitializationDataSource?>(null)
    val initializationDataSource: Deferred<CustomerSheetInitializationDataSource>
        get() = _initializationDataSource.asDeferred()

    private val _paymentMethodDataSource = MutableStateFlow<CustomerSheetPaymentMethodDataSource?>(null)
    val paymentMethodDataSource: Deferred<CustomerSheetPaymentMethodDataSource>
        get() = _paymentMethodDataSource.asDeferred()

    private val _savedSelectionDataSource = MutableStateFlow<CustomerSheetSavedSelectionDataSource?>(null)
    val savedSelectionDataSource: Deferred<CustomerSheetSavedSelectionDataSource>
        get() = _savedSelectionDataSource.asDeferred()

    private val _intentDataSource = MutableStateFlow<CustomerSheetIntentDataSource?>(null)
    val intentDataSource: Deferred<CustomerSheetIntentDataSource>
        get() = _intentDataSource.asDeferred()

    fun initialize(
        application: Application,
        lifecycleOwner: LifecycleOwner,
        integration: CustomerSheetIntegration,
    ) {
        when (integration) {
            is CustomerSheetIntegration.Adapter -> {
                val adapterDataSourceComponent = DaggerCustomerAdapterDataSourceComponent
                    .builder()
                    .application(application)
                    .adapter(integration.adapter)
                    .build()

                _initializationDataSource.value = adapterDataSourceComponent.customerSheetInitializationDataSource
                _paymentMethodDataSource.value = adapterDataSourceComponent.customerSheetPaymentMethodDataSource
                _intentDataSource.value = adapterDataSourceComponent.customerSheetIntentDataSource
                _savedSelectionDataSource.value = adapterDataSourceComponent.customerSheetSavedSelectionDataSource
            }
            is CustomerSheetIntegration.CustomerSession -> {
                val customerSessionDataSourceComponent = DaggerCustomerSessionDataSourceComponent
                    .builder()
                    .application(application)
                    .customerSessionProvider(integration.customerSessionProvider)
                    .build()

                _initializationDataSource.value =
                    customerSessionDataSourceComponent.customerSheetInitializationDataSource
                _paymentMethodDataSource.value =
                    customerSessionDataSourceComponent.customerSheetPaymentMethodDataSource
                _intentDataSource.value =
                    customerSessionDataSourceComponent.customerSheetIntentDataSource
                _savedSelectionDataSource.value =
                    customerSessionDataSourceComponent.customerSheetSavedSelectionDataSource
            }
        }

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    val isChangingConfigurations = when (owner) {
                        is ComponentActivity -> owner.isChangingConfigurations
                        is Fragment -> owner.activity?.isChangingConfigurations ?: false
                        else -> false
                    }

                    if (!isChangingConfigurations) {
                        clear()
                    }

                    super.onDestroy(owner)
                }
            }
        )
    }

    fun clear() {
        _initializationDataSource.value = null
        _paymentMethodDataSource.value = null
        _savedSelectionDataSource.value = null
        _intentDataSource.value = null
    }
}

private fun <T : Any> Flow<T?>.asDeferred(): Deferred<T> {
    val deferred = CompletableDeferred<T>()

    // Prevent casting to CompletableDeferred and manual completion.
    return object : Deferred<T> by deferred {
        override suspend fun await(): T {
            return this@asDeferred.filterNotNull().first()
        }
    }
}
