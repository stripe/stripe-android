package com.stripe.android.googlepaysheet

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

internal interface GooglePaySheetLauncher {
    suspend fun configure(
        configuration: GooglePaySheetConfig
    ): Boolean

    fun present()
}

internal class DefaultGooglePaySheetLauncher(
    viewModelStoreOwner: ViewModelStoreOwner,
    private val ioContext: CoroutineContext,
    private val googlePayRepositoryFactory: (GooglePaySheetEnvironment) -> GooglePayRepository,
    private val activityResultLauncher: ActivityResultLauncher<StripeGooglePayContract.Args>
) : GooglePaySheetLauncher {

    private val viewModel =
        ViewModelProvider(viewModelStoreOwner)[GooglePaySheetConfigureViewModel::class.java]

    constructor(
        activity: ComponentActivity,
        ioContext: CoroutineContext = Dispatchers.IO,
        googlePayRepositoryFactory: (GooglePaySheetEnvironment) -> GooglePayRepository,
        callback: GooglePaySheetResultCallback
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
        googlePayRepositoryFactory: (GooglePaySheetEnvironment) -> GooglePayRepository,
        callback: GooglePaySheetResultCallback
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
        googlePayRepositoryFactory: (GooglePaySheetEnvironment) -> GooglePayRepository,
        callback: GooglePaySheetResultCallback
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
        configuration: GooglePaySheetConfig
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
            "GooglePaySheet must be successfully initialized using configure() before calling present()."
    }
}
