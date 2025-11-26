package com.stripe.android.common.taptoadd

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.RestrictTo
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.core.exception.StripeException
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.callable.ReaderCallback
import com.stripe.stripeterminal.external.callable.TapToPayReaderListener
import com.stripe.stripeterminal.external.callable.TerminalListener
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.ConnectionTokenException
import com.stripe.stripeterminal.external.models.DeviceType
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext

internal interface TapToAddConnectionManager {
    val isSupported: Boolean
    val isConnected: Boolean

    fun connect()

    companion object {
        fun create(
            isStripeTerminalSdkAvailable: IsStripeTerminalSdkAvailable,
            terminalWrapper: TerminalWrapper,
            errorReporter: ErrorReporter,
            applicationContext: Context,
            workContext: CoroutineContext,
            isSimulated: Boolean,
        ): TapToAddConnectionManager {
            return if (isStripeTerminalSdkAvailable()) {
                DefaultTapToAddConnectionManager(
                    applicationContext = applicationContext,
                    workContext = workContext,
                    errorReporter = errorReporter,
                    terminalWrapper = terminalWrapper,
                    isSimulated = isSimulated,
                )
            } else {
                UnsupportedTapToAddConnectionManager()
            }
        }
    }
}

@OptIn(TapToAddPreview::class)
internal class DefaultTapToAddConnectionManager(
    applicationContext: Context,
    workContext: CoroutineContext,
    private val errorReporter: ErrorReporter,
    private val terminalWrapper: TerminalWrapper,
    isSimulated: Boolean,
) : TapToAddConnectionManager, TerminalListener, TapToPayReaderListener {
    private var connectionTask: CompletableDeferred<Boolean>? = null

    private val discoveryConfiguration = DiscoveryConfiguration.TapToPayDiscoveryConfiguration(isSimulated)
    private val workScope = CoroutineScope(workContext)
    private val connectionStartLock = Mutex()

    override val isSupported: Boolean
        get() {
            return terminal().supportsReadersOfType(
                deviceType = DeviceType.TAP_TO_PAY_DEVICE,
                discoveryConfiguration = discoveryConfiguration,
            ).isSupported
        }

    override val isConnected: Boolean
        get() = terminal().connectedReader != null

    init {
        if (!terminalWrapper.isInitialized()) {
            terminalWrapper.initTerminal(
                context = applicationContext,
                tokenProvider = object : ConnectionTokenProvider {
                    override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
                        workScope.launch {
                            val createConnectionTokenCallback = TerminalConnectionTokenCallbackHolder.get()
                                ?: run {
                                    val message = "No connection token callback was initialized!"

                                    callback.onFailure(
                                        ConnectionTokenException(
                                            message = message,
                                            cause = IllegalStateException(message)
                                        )
                                    )

                                    return@launch
                                }

                            when (val result = createConnectionTokenCallback.createConnectionToken()) {
                                is CreateConnectionTokenResult.Success -> {
                                    callback.onSuccess(result.connectionToken)
                                }
                                is CreateConnectionTokenResult.Failure -> {
                                    callback.onFailure(
                                        ConnectionTokenException(
                                            message = result.message,
                                            cause = result.cause,
                                        )
                                    )
                                }
                            }
                        }
                    }
                },
                listener = this,
            )
        }
    }

    override fun connect() {
        workScope.launch {
            val canContinue = connectionStartLock.withLock {
                if (!isSupported || isConnected || connectionTask?.isActive == true) {
                    false
                } else {
                    connectionTask = CompletableDeferred()
                    true
                }
            }

            if (!canContinue) {
                return@launch
            }

            try {
                discoverReaders()
            } catch (exception: SecurityException) {
                errorReporter.report(
                    ErrorReporter.UnexpectedErrorEvent.TAP_TO_ADD_LOCATION_PERMISSIONS_FAILURE,
                    StripeException.create(exception),
                )

                connectionTask?.completeExceptionally(exception)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun discoverReaders() {
        terminal().discoverReaders(
            config = discoveryConfiguration,
            discoveryListener = object : DiscoveryListener {
                override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
                    onUpdatedReaders(readers)
                }
            },
            callback = object : Callback {
                override fun onFailure(e: TerminalException) {
                    errorReporter.report(
                        ErrorReporter.ExpectedErrorEvent.TAP_TO_ADD_DISCOVER_READERS_CALL_FAILURE,
                        StripeException.create(e),
                    )

                    connectionTask?.completeExceptionally(e)
                    connectionTask = null
                }

                override fun onSuccess() {
                    errorReporter.report(ErrorReporter.SuccessEvent.TAP_TO_ADD_DISCOVER_READERS_CALL_SUCCESS)
                }
            }
        )
    }

    private fun onUpdatedReaders(readers: List<Reader>) {
        val reader = readers.firstOrNull() ?: run {
            /*
             * The Tap to Pay variant should never not return a reader through this callback.
             * If no readers are found, something has changed in the internal implementation
             * of Terminal that we should know about
             */
            errorReporter.report(
                ErrorReporter.UnexpectedErrorEvent.TAP_TO_ADD_NO_READER_FOUND,
            )

            connectionTask?.completeExceptionally(IllegalStateException("No reader found!"))
            return
        }

        val locationId = TerminalLocationHolder.locationId ?: run {
            connectionTask?.completeExceptionally(IllegalStateException("No location specified!"))
            return
        }

        terminal().connectReader(
            reader = reader,
            config = ConnectionConfiguration.TapToPayConnectionConfiguration(
                locationId = locationId,
                autoReconnectOnUnexpectedDisconnect = true,
                tapToPayReaderListener = this@DefaultTapToAddConnectionManager,
            ),
            connectionCallback = object : ReaderCallback {
                override fun onFailure(e: TerminalException) {
                    errorReporter.report(
                        ErrorReporter.ExpectedErrorEvent.TAP_TO_ADD_CONNECT_READER_CALL_FAILURE,
                        StripeException.create(e),
                    )

                    connectionTask?.completeExceptionally(e)
                    connectionTask = null
                }

                override fun onSuccess(reader: Reader) {
                    errorReporter.report(
                        ErrorReporter.SuccessEvent.TAP_TO_ADD_CONNECT_READER_CALL_SUCCESS
                    )

                    connectionTask?.complete(true)
                    connectionTask = null
                }
            }
        )
    }

    private fun terminal() = terminalWrapper.getInstance()
}

internal class UnsupportedTapToAddConnectionManager : TapToAddConnectionManager {
    override val isSupported: Boolean = false
    override val isConnected: Boolean = false

    override fun connect() {
        // No-op
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@TapToAddPreview
fun interface CreateConnectionTokenCallback {
    suspend fun createConnectionToken(): CreateConnectionTokenResult
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@TapToAddPreview
sealed interface CreateConnectionTokenResult {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Success(
        internal val connectionToken: String,
    ) : CreateConnectionTokenResult

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Failure(
        internal val cause: Exception,
        internal val message: String,
    ) : CreateConnectionTokenResult
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@TapToAddPreview
object TerminalConnectionTokenCallbackHolder {
    private var createConnectionTokenCallback: CreateConnectionTokenCallback? = null

    fun get() = createConnectionTokenCallback

    fun set(callback: CreateConnectionTokenCallback, lifecycleOwner: LifecycleOwner) {
        createConnectionTokenCallback = callback

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    createConnectionTokenCallback = null
                }
            }
        )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@TapToAddPreview
object TerminalLocationHolder {
    var locationId: String? = null
}
