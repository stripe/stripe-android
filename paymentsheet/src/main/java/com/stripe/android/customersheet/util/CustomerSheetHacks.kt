package com.stripe.android.customersheet.util

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi

/**
 * This objects holds references to objects that need to be shared across Activity boundaries
 * but can't be serialized, or objects that can't be injected where they are used.
 */
@OptIn(ExperimentalCustomerSheetApi::class)
internal object CustomerSheetHacks {

    private var adapter: CustomerAdapter? = null
    private var configuration: CustomerSheet.Configuration? = null

    fun requireAdapter(): CustomerAdapter {
        return requireNotNull(adapter) {
            "No adapter set on CustomerSheetHacks"
        }
    }

    fun requireConfiguration(): CustomerSheet.Configuration {
        return requireNotNull(configuration) {
            "No configuration set on CustomerSheetHacks"
        }
    }

    fun initialize(
        lifecycleOwner: LifecycleOwner,
        adapter: CustomerAdapter,
        configuration: CustomerSheet.Configuration,
    ) {
        this.adapter = adapter
        this.configuration = configuration

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
        this.adapter = null
        this.configuration = null
    }
}
