package com.stripe.android.iap

import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dev.drewhamilton.poko.Poko

// TODO: Supply publishable key
class InAppPurchase(
    private val publishableKey: String,
    private val activity: ComponentActivity,
    private val resultCallback: ResultCallback,
    private val plugins: List<InAppPurchasePlugin>,
) {
    private val viewModel: InAppPurchaseViewModel by activity.viewModels()

    init {
        require(plugins.isNotEmpty())

        activity.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    for (plugin in plugins) {
                        plugin.register(activity, activity, resultCallback)
                    }
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    for (plugin in plugins) {
                        plugin.unregister()
                    }
                }
            }
        )
    }

    fun purchase(
        priceId: String,
        lookupType: InAppPurchasePluginLookupType = InAppPurchasePluginLookupType.Default(),
    ) {
        viewModel.lookup(
            lookupType = lookupType,
            plugins = plugins,
            priceId = priceId,
        )
    }

    sealed interface Result {
        class Completed internal constructor() : Result
        class Canceled internal constructor() : Result

        @Poko
        class Failed internal constructor(val error: Throwable) : Result
    }

    fun interface ResultCallback {
        fun onResult(result: Result)
    }
}
