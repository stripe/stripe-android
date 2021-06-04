package com.stripe.android.googlepaysheet

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.stripe.android.paymentsheet.DefaultGooglePayRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A drop-in class that presents a Google Pay sheet to collect a customer's payment.
 */
internal class GooglePaySheet internal constructor(
    private val lifecycleScope: () -> CoroutineScope,
    private val googlePaySheetLauncher: GooglePaySheetLauncher
) {
    /**
     * Constructor to be used when launching GooglePaySheet from an Activity.
     *
     * @param activity the Activity that is launching the GooglePaySheet
     * @param callback called with the result of the GooglePaySheet operation
     */
    internal constructor(
        activity: ComponentActivity,
        callback: GooglePaySheetResultCallback
    ) : this(
        { activity.lifecycleScope },
        DefaultGooglePaySheetLauncher(
            activity,
            googlePayRepositoryFactory = {
                DefaultGooglePayRepository(
                    activity.application,
                    it
                )
            },
            callback = callback
        )
    )

    /**
     * Constructor to be used when launching GooglePaySheet from an Activity.
     *
     * @param fragment the Fragment that is launching the GooglePaySheet
     * @param callback called with the result of the GooglePaySheet operation
     */
    internal constructor(
        fragment: Fragment,
        callback: GooglePaySheetResultCallback
    ) : this(
        { fragment.viewLifecycleOwner.lifecycleScope },
        DefaultGooglePaySheetLauncher(
            fragment,
            googlePayRepositoryFactory = {
                DefaultGooglePayRepository(
                    fragment.requireActivity().application,
                    it
                )
            },
            callback = callback
        )
    )

    fun configure(
        configuration: GooglePaySheetConfig,
        callback: ConfigCallback
    ) {
        lifecycleScope().launch {
            runCatching {
                googlePaySheetLauncher.configure(configuration)
            }.fold(
                onSuccess = {
                    callback.onConfigured(it, null)
                },
                onFailure = {
                    callback.onConfigured(false, it)
                }
            )
        }
    }

    fun present() {
        googlePaySheetLauncher.present()
    }

    fun interface ConfigCallback {
        fun onConfigured(
            success: Boolean,
            error: Throwable?
        )
    }
}
