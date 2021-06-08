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
internal class GooglePayLauncher internal constructor(
    private val lifecycleScope: () -> CoroutineScope,
    private val googlePayController: GooglePayController
) {
    /**
     * Constructor to be used when launching [GooglePayLauncher] from an Activity.
     *
     * @param activity the Activity that is launching the [GooglePayLauncher]
     * @param callback called with the result of the [GooglePayLauncher] operation
     */
    internal constructor(
        activity: ComponentActivity,
        callback: ResultCallback
    ) : this(
        { activity.lifecycleScope },
        DefaultGooglePayController(
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
     * Constructor to be used when launching [GooglePayLauncher] from an Activity.
     *
     * @param fragment the Fragment that is launching the [GooglePayLauncher]
     * @param callback called with the result of the [GooglePayLauncher] operation
     */
    internal constructor(
        fragment: Fragment,
        callback: ResultCallback
    ) : this(
        { fragment.viewLifecycleOwner.lifecycleScope },
        DefaultGooglePayController(
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
        configuration: GooglePayConfig,
        callback: ConfigCallback
    ) {
        lifecycleScope().launch {
            runCatching {
                googlePayController.configure(configuration)
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
        googlePayController.present()
    }

    fun interface ConfigCallback {
        fun onConfigured(
            success: Boolean,
            error: Throwable?
        )
    }

    internal fun interface ResultCallback {
        fun onResult(result: GooglePayLauncherResult)
    }
}
