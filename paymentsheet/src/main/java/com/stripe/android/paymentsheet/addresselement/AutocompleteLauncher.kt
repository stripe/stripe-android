package com.stripe.android.paymentsheet.addresselement

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.ManagedAddressManager

internal interface AutocompleteLauncher {
    val googlePlacesApiKey: String

    fun launch(id: String, country: String, onResult: (ManagedAddressManager.State) -> Unit)
}

internal interface AutocompleteActivityLauncher : AutocompleteLauncher{
    fun register(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner)
}

internal class DefaultAutocompleteLauncher(
    override val googlePlacesApiKey: String,
    private val appearance: PaymentSheet.Appearance,
) : AutocompleteActivityLauncher {
    private var activityLauncher: ActivityResultLauncher<AutocompleteContract.Args>? = null

    private val registeredAutocompleteListeners = mutableMapOf<String, (ManagedAddressManager.State) -> Unit>()

    override fun register(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner) {
        activityLauncher = activityResultCaller.registerForActivityResult(
            AutocompleteContract
        ) { result ->
            val values = result.addressDetails?.toIdentifierMap()

            registeredAutocompleteListeners[result.id]?.invoke(
                if (result.forceExpandForm) {
                    ManagedAddressManager.State.Expanded(values)
                } else if (values == null) {
                    ManagedAddressManager.State.Condensed
                } else {
                    ManagedAddressManager.State.Expanded(values)
                }
            )

            registeredAutocompleteListeners.remove(result.id)
        }

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    activityLauncher?.unregister()
                    activityLauncher = null
                }
            }
        )
    }

    override fun launch(
        id: String,
        country: String,
        onResult: (ManagedAddressManager.State) -> Unit
    ) {
        registeredAutocompleteListeners[id] = onResult

        activityLauncher?.launch(
            AutocompleteContract.Args(
                id = id,
                country = country,
                googlePlacesApiKey = googlePlacesApiKey,
                appearance = appearance,
            )
        )
    }
}
