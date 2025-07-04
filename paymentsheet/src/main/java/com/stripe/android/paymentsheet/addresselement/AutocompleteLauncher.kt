package com.stripe.android.paymentsheet.addresselement

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.parcelize.Parcelize
import java.lang.ref.WeakReference
import java.util.UUID

internal interface AutocompleteLauncher {
    fun launch(
        country: String,
        googlePlacesApiKey: String,
        resultHandler: AutocompleteLauncherResultHandler
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

internal fun interface AutocompleteLauncherResultHandler {
    fun onAutocompleteLauncherResult(result: AutocompleteLauncher.Result)
}

internal class DefaultAutocompleteLauncher(
    private val appearance: PaymentSheet.Appearance,
) : AutocompleteActivityLauncher {
    private var activityLauncher: ActivityResultLauncher<AutocompleteContract.Args>? = null

    private val registeredAutocompleteListeners =
        mutableMapOf<String, WeakReference<AutocompleteLauncherResultHandler>>()

    override fun register(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner) {
        activityLauncher = activityResultCaller.registerForActivityResult(
            AutocompleteContract
        ) { result ->
            registeredAutocompleteListeners[result.id]?.get()?.onAutocompleteLauncherResult(
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
        resultHandler: AutocompleteLauncherResultHandler
    ) {
        val id = UUID.randomUUID().toString()

        registeredAutocompleteListeners[id] = WeakReference(resultHandler)

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
