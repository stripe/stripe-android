package com.stripe.android.customersheet.util

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.customersheet.data.CustomerAdapterDataSource
import com.stripe.android.customersheet.data.CustomerSheetCombinedDataSource
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
@OptIn(ExperimentalCustomerSheetApi::class)
internal object CustomerSheetHacks {

    private val _adapter = MutableStateFlow<CustomerAdapter?>(null)
    val adapter: Deferred<CustomerAdapter>
        get() = _adapter.asDeferred()

    private val _dataSource = MutableStateFlow<CustomerSheetCombinedDataSource?>(null)
    val dataSource: Deferred<CustomerSheetCombinedDataSource>
        get() = _dataSource.asDeferred()

    fun initialize(
        lifecycleOwner: LifecycleOwner,
        adapter: CustomerAdapter,
    ) {
        _adapter.value = adapter
        _dataSource.value = CustomerAdapterDataSource(adapter)

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
        _adapter.value = null
        _dataSource.value = null
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
