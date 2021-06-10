package com.stripe.android.googlepaylauncher

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.paymentsheet.GooglePayRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.TestOnly
import kotlin.coroutines.CoroutineContext

internal interface GooglePayController {
    suspend fun configure(
        configuration: GooglePayConfig
    ): Boolean

    fun present()
}

/**
 * Default [GooglePayController] for managing checks for Google Pay readiness and starting
 * [StripeGooglePayActivity] using [StripeGooglePayContract.Args].
 */
internal class DefaultGooglePayController(
    viewModelStoreOwner: ViewModelStoreOwner,
    private val ioContext: CoroutineContext,
    private val googlePayRepositoryFactory: (GooglePayEnvironment) -> GooglePayRepository,
    private val activityResultLauncher: ActivityResultLauncher<StripeGooglePayContract.Args>
) : GooglePayController {

    private val viewModel =
        ViewModelProvider(viewModelStoreOwner)[GooglePayLauncherConfigureViewModel::class.java]

    constructor(
        activity: ComponentActivity,
        ioContext: CoroutineContext = Dispatchers.IO,
        googlePayRepositoryFactory: (GooglePayEnvironment) -> GooglePayRepository,
        callback: GooglePayLauncher.ResultCallback
    ) : this(
        activity,
        ioContext,
        googlePayRepositoryFactory,
        activity.registerForActivityResult(
            StripeGooglePayContract()
        ) {
            callback.onResult(it)
        }
    )

    constructor(
        fragment: Fragment,
        ioContext: CoroutineContext = Dispatchers.IO,
        googlePayRepositoryFactory: (GooglePayEnvironment) -> GooglePayRepository,
        callback: GooglePayLauncher.ResultCallback
    ) : this(
        fragment,
        ioContext,
        googlePayRepositoryFactory,
        fragment.registerForActivityResult(
            StripeGooglePayContract()
        ) {
            callback.onResult(it)
        }
    )

    @TestOnly
    constructor(
        fragment: Fragment,
        registry: ActivityResultRegistry,
        ioContext: CoroutineContext = Dispatchers.IO,
        googlePayRepositoryFactory: (GooglePayEnvironment) -> GooglePayRepository,
        callback: GooglePayLauncher.ResultCallback
    ) : this(
        fragment,
        ioContext,
        googlePayRepositoryFactory,
        fragment.registerForActivityResult(
            StripeGooglePayContract(),
            registry
        ) {
            callback.onResult(it)
        }
    )

    override suspend fun configure(
        configuration: GooglePayConfig
    ): Boolean = withContext(ioContext) {
        val repository = googlePayRepositoryFactory(configuration.environment)

        val isReady = repository.isReady().first()
        if (isReady) {
            viewModel.setArgs(
                StripeGooglePayContract.Args(
                    config = configuration,
                    statusBarColor = null
                )
            )

            true
        } else {
            throw RuntimeException("Google Pay is not available.")
        }
    }

    override fun present() {
        val args = runCatching {
            viewModel.args
        }.getOrElse {
            error(CONFIGURE_ERROR)
        }

        activityResultLauncher.launch(args)
    }

    private companion object {
        private const val CONFIGURE_ERROR =
            "GooglePayLauncher must be successfully initialized using configure() before calling present()."
    }
}
