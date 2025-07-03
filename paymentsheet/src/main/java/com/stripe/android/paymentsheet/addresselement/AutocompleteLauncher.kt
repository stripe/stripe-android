package com.stripe.android.paymentsheet.addresselement

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.parcelize.Parcelize
import java.util.UUID

internal interface AutocompleteLauncher {
    fun launch(
        country: String,
        googlePlacesApiKey: String,
        onResult: (Result) -> Unit
    )

    sealed interface Result : Parcelable {
        val addressDetails: AddressDetails?

        @Parcelize
        data class EnterManually(override val addressDetails: AddressDetails?) : Result

        @Parcelize
        data class OnBack(override val addressDetails: AddressDetails?) : Result
    }
}

internal interface AutocompleteActivityLauncher : AutocompleteLauncher {
    fun register(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner)
}

internal class DefaultAutocompleteLauncher(
    private val appearance: PaymentSheet.Appearance,
) : AutocompleteActivityLauncher {
    private var activityLauncher: ActivityResultLauncher<AutocompleteContract.Args>? = null

    private val registeredAutocompleteListeners = mutableMapOf<String, (AutocompleteLauncher.Result) -> Unit>()

    override fun register(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner) {
        activityLauncher = activityResultCaller.registerForActivityResult(
            AutocompleteContract
        ) { result ->
            registeredAutocompleteListeners[result.id]?.invoke(
                when (result) {
                    is AutocompleteContract.Result.EnterManually -> AutocompleteLauncher.Result.EnterManually(
                        result.addressDetails,
                    )
                    is AutocompleteContract.Result.Address -> AutocompleteLauncher.Result.OnBack(
                        result.addressDetails
                    )
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
        country: String,
        googlePlacesApiKey: String,
        onResult: (AutocompleteLauncher.Result) -> Unit
    ) {
        val id = UUID.randomUUID().toString()

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
